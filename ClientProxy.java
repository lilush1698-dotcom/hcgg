package kirill.jungleairship;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

/**
 * Дирижабль: палуба 6x6 (твёрдая), шар и корпус рисуются как блоки. На палубу можно ставить блоки
 * (ПКМ блоком), они тянут корабль вниз и смещают центр масс (крен/дифферент).
 * Пилот: W/S тяга, A/D поворот, Space вверх, Ctrl вниз. Топливо: уголь/щепки и т.п. (ПКМ предметом).
 */
public class EntityAirship extends Entity implements IEntityAdditionalSpawnData {
	public static final int MAX_FUEL = 24000;
	private static final DataParameter<Integer> FUEL = EntityDataManager.createKey(EntityAirship.class, DataSerializers.VARINT);
	private static final DataParameter<Float> ROLL = EntityDataManager.createKey(EntityAirship.class, DataSerializers.FLOAT);
	private static final double[][] SEATS = { { 0.0, -2.0 }, { -1.4, -0.5 }, { 1.4, 1.2 }, { 0.0, 0.6 } };

	public static class Cell {
		public final BlockPos pos;
		public final IBlockState state;

		public Cell(BlockPos pos, IBlockState state) {
			this.pos = pos;
			this.state = state;
		}
	}

	public final List<Cell> placed = new ArrayList<Cell>();

	private boolean cFwd, cBack, cLeft, cRight, cUp, cDown;
	private double lastX, lastY, lastZ;
	private float lastYaw;
	private boolean lastInit;
	private float yawVel;
	private double comX, comZ;
	public float prevRoll;

	private int lerpSteps;
	private double lerpX, lerpY, lerpZ, lerpYaw, lerpPitch;

	public EntityAirship(World w) {
		super(w);
		this.setSize(6.0F, 1.0F);
		this.preventEntitySpawning = true;
		this.ignoreFrustumCheck = true;
		this.isImmuneToFire = true;
	}

	@Override
	protected void entityInit() {
		this.dataManager.register(FUEL, Integer.valueOf(0));
		this.dataManager.register(ROLL, Float.valueOf(0.0F));
	}

	public int getFuel() {
		return this.dataManager.get(FUEL).intValue();
	}

	public void setFuel(int f) {
		this.dataManager.set(FUEL, Integer.valueOf(MathHelper.clamp(f, 0, MAX_FUEL)));
	}

	public float getRoll() {
		return this.dataManager.get(ROLL).floatValue();
	}

	public void setControls(int flags, EntityPlayer from) {
		if (getControllingPassenger() != from) {
			return;
		}
		cFwd = (flags & 1) != 0;
		cBack = (flags & 2) != 0;
		cLeft = (flags & 4) != 0;
		cRight = (flags & 8) != 0;
		cUp = (flags & 16) != 0;
		cDown = (flags & 32) != 0;
	}

	// ---------- столкновения и посадка ----------
	@Override
	public AxisAlignedBB getCollisionBoundingBox() {
		return this.getEntityBoundingBox();
	}

	@Override
	public boolean canBeCollidedWith() {
		return !this.isDead;
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	@Override
	public boolean canPassengerSteer() {
		return false; // всей физикой управляет сервер
	}

	@Override
	protected boolean canFitPassenger(Entity p) {
		return this.getPassengers().size() < 4;
	}

	@Override
	public boolean canRiderInteract() {
		return true;
	}

	@Override
	public double getMountedYOffset() {
		return 1.0D;
	}

	@Override
	public Entity getControllingPassenger() {
		List<Entity> l = this.getPassengers();
		return l.isEmpty() ? null : l.get(0);
	}

	@Override
	public void updatePassenger(Entity passenger) {
		if (!this.isPassenger(passenger)) {
			return;
		}
		int idx = Math.min(this.getPassengers().indexOf(passenger), 3);
		if (idx < 0) {
			idx = 0;
		}
		double yawRad = Math.toRadians(this.rotationYaw);
		double sx = SEATS[idx][0];
		double sz = SEATS[idx][1];
		double wx = sx * Math.cos(yawRad) - sz * Math.sin(yawRad);
		double wz = sx * Math.sin(yawRad) + sz * Math.cos(yawRad);
		passenger.setPosition(this.posX + wx, this.posY + this.getMountedYOffset() + passenger.getYOffset(), this.posZ + wz);
	}

	@Override
	public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int inc, boolean teleport) {
		this.lerpX = x;
		this.lerpY = y;
		this.lerpZ = z;
		this.lerpYaw = yaw;
		this.lerpPitch = pitch;
		this.lerpSteps = 3;
	}

	// ---------- преобразование координат корабль <-> мир (по рысканию) ----------
	public Vec3d toLocal(double wx, double wz, double wy) {
		double yr = Math.toRadians(this.rotationYaw);
		double lx = wx * Math.cos(yr) + wz * Math.sin(yr);
		double lz = -wx * Math.sin(yr) + wz * Math.cos(yr);
		return new Vec3d(lx, wy, lz);
	}

	// ---------- тик ----------
	@Override
	public void onUpdate() {
		super.onUpdate();
		this.prevRoll = this.getRoll();
		if (this.world.isRemote) {
			this.clientLerp();
		} else {
			this.serverTick();
		}
		this.afterMove();
		if (this.world.isRemote && this.getFuel() > 0 && this.ticksExisted % 3 == 0) {
			double yr = Math.toRadians(this.rotationYaw);
			double lx = -1.5, lz = 0.5;
			double wx = lx * Math.cos(yr) - lz * Math.sin(yr);
			double wz = lx * Math.sin(yr) + lz * Math.cos(yr);
			this.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, this.posX + wx, this.posY + 4.6, this.posZ + wz, 0.0, 0.05, 0.0);
		}
	}

	private void clientLerp() {
		if (this.lerpSteps > 0) {
			double nx = this.posX + (this.lerpX - this.posX) / this.lerpSteps;
			double ny = this.posY + (this.lerpY - this.posY) / this.lerpSteps;
			double nz = this.posZ + (this.lerpZ - this.posZ) / this.lerpSteps;
			float ny2 = (float) (this.rotationYaw + MathHelper.wrapDegrees(this.lerpYaw - this.rotationYaw) / this.lerpSteps);
			float np = (float) (this.rotationPitch + (this.lerpPitch - this.rotationPitch) / this.lerpSteps);
			this.lerpSteps--;
			this.setPosition(nx, ny, nz);
			this.setRotation(ny2, np);
		}
	}

	private void serverTick() {
		Entity pilot = this.getControllingPassenger();
		if (pilot == null) {
			cFwd = cBack = cLeft = cRight = cUp = cDown = false;
		}
		int n = this.placed.size();
		double mass = 1.0 + n / 60.0;
		int fuel = this.getFuel();
		boolean lit = fuel > 0;

		if (lit) {
			int burn = 1;
			if (cFwd || cBack) {
				burn += 1;
			}
			if (cUp) {
				burn += 2;
			}
			this.setFuel(fuel - burn);
		}

		double targetVy;
		if (lit) {
			targetVy = cUp ? 0.22 : (cDown ? -0.22 : 0.0);
			targetVy -= Math.max(0, n - 100) * 0.0015;
		} else {
			targetVy = this.onGround ? 0.0 : -0.12;
		}
		this.motionY += (targetVy - this.motionY) * 0.12;

		double accel = lit ? 0.05 / mass : 0.0;
		double f = (cFwd ? 1.0 : 0.0) - (cBack ? 0.5 : 0.0);
		double yr = Math.toRadians(this.rotationYaw);
		this.motionX += -Math.sin(yr) * accel * f;
		this.motionZ += Math.cos(yr) * accel * f;
		this.motionX *= 0.96;
		this.motionZ *= 0.96;
		double sp = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
		if (sp > 0.9) {
			this.motionX *= 0.9 / sp;
			this.motionZ *= 0.9 / sp;
			sp = 0.9;
		}

		double steer = (cRight ? 1.0 : 0.0) - (cLeft ? 1.0 : 0.0);
		this.yawVel += steer * 0.35 * (0.4 + Math.min(1.0, sp / 0.4) * 0.6);
		this.yawVel *= 0.85F;
		this.rotationYaw += this.yawVel;

		// крен и дифферент: от центра масс поставленных блоков и от поворота
		float targetRoll = (float) MathHelper.clamp(comX * 2.0 - this.yawVel * 1.5, -9.0, 9.0);
		float targetPitch = (float) MathHelper.clamp(-comZ * 2.0 - this.motionY * 10.0, -9.0, 9.0);
		this.dataManager.set(ROLL, Float.valueOf(this.getRoll() + (targetRoll - this.getRoll()) * 0.1F));
		this.rotationPitch += (targetPitch - this.rotationPitch) * 0.1F;

		this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
		if (this.collidedHorizontally) {
			this.motionX *= 0.5;
			this.motionZ *= 0.5;
		}
		if (this.onGround && !lit) {
			this.motionY = 0.0;
		}

		if (this.ticksExisted % 10 == 0 && pilot instanceof EntityPlayer) {
			int pct = (int) Math.round(100.0 * this.getFuel() / MAX_FUEL);
			String s = String.format("Топливо: %d%%   Скорость: %.1f бл/с   Блоков: %d/%d", pct, sp * 20.0, n, ModConfig.maxBlocks);
			((EntityPlayer) pilot).sendStatusMessage(new TextComponentString(s), true);
		}
	}

	/** Переносит сущности, стоящие на палубе, вместе с кораблём (поворот + сдвиг). */
	private void afterMove() {
		double dx = this.posX - this.lastX;
		double dy = this.posY - this.lastY;
		double dz = this.posZ - this.lastZ;
		float dyaw = this.rotationYaw - this.lastYaw;
		if (this.lastInit && (dx != 0 || dy != 0 || dz != 0 || dyaw != 0)) {
			AxisAlignedBB box = this.getEntityBoundingBox();
			AxisAlignedBB zone = new AxisAlignedBB(box.minX, box.maxY - 0.3, box.minZ, box.maxX, box.maxY + 0.7, box.maxZ);
			double c = Math.cos(Math.toRadians(dyaw));
			double s = Math.sin(Math.toRadians(dyaw));
			for (Entity e : this.world.getEntitiesWithinAABBExcludingEntity(this, zone)) {
				if (e.isRiding() || e.isDead || e instanceof EntityAirship || e.noClip) {
					continue;
				}
				if (e instanceof EntityPlayer) {
					EntityPlayer p = (EntityPlayer) e;
					if (!this.world.isRemote || p != JungleAirshipMod.proxy.getLocalPlayer() || p.capabilities.isFlying) {
						continue; // игрока двигает его же клиент
					}
				}
				double rx = e.posX - this.posX;
				double rz = e.posZ - this.posZ;
				double nx = rx * c - rz * s;
				double nz = rx * s + rz * c;
				e.move(MoverType.SELF, (nx - rx) + dx, dy, (nz - rz) + dz);
				e.fallDistance = 0.0F;
				e.rotationYaw += dyaw;
			}
		}
		this.lastX = this.posX;
		this.lastY = this.posY;
		this.lastZ = this.posZ;
		this.lastYaw = this.rotationYaw;
		this.lastInit = true;
	}

	// ---------- взаимодействие ----------
	@Override
	public EnumActionResult applyPlayerInteraction(EntityPlayer player, Vec3d vec, EnumHand hand) {
		if (this.world.isRemote) {
			return EnumActionResult.SUCCESS;
		}
		if (ModAccess.isDisabled(player)) {
			player.sendStatusMessage(new TextComponentString("Мод Jungle Airship отключён для тебя администратором."), true);
			return EnumActionResult.SUCCESS;
		}
		ItemStack st = player.getHeldItem(hand);
		if (st.isEmpty()) {
			if (player.isSneaking()) {
				this.removeLast(player);
			} else {
				player.startRiding(this);
			}
			return EnumActionResult.SUCCESS;
		}
		Item item = st.getItem();
		int burn = TileEntityFurnace.getItemBurnTime(st);
		if (burn > 0 && (!(item instanceof ItemBlock) || player.isSneaking())) {
			this.addFuel(player, hand, st, burn);
			return EnumActionResult.SUCCESS;
		}
		if (item instanceof ItemBlock) {
			this.tryPlace(player, hand, st, vec);
		}
		return EnumActionResult.SUCCESS;
	}

	private void addFuel(EntityPlayer p, EnumHand hand, ItemStack st, int burn) {
		int f = this.getFuel();
		if (f >= MAX_FUEL) {
			p.sendStatusMessage(new TextComponentString("Бак полон"), true);
			return;
		}
		this.setFuel(f + burn);
		if (!p.isCreative()) {
			if (st.getItem() == Items.LAVA_BUCKET) {
				p.setHeldItem(hand, new ItemStack(Items.BUCKET));
			} else {
				st.shrink(1);
			}
		}
		this.world.playSound(null, this.posX, this.posY, this.posZ, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.NEUTRAL, 1.0F, 1.0F);
		p.sendStatusMessage(new TextComponentString("Топливо: " + Math.round(100.0 * this.getFuel() / MAX_FUEL) + "%"), true);
	}

	private void tryPlace(EntityPlayer p, EnumHand hand, ItemStack st, Vec3d vec) {
		Block b = ((ItemBlock) st.getItem()).getBlock();
		IBlockState s = b.getStateFromMeta(st.getMetadata());
		if (b.hasTileEntity(s) || s.getRenderType() != EnumBlockRenderType.MODEL) {
			p.sendStatusMessage(new TextComponentString("Этот блок нельзя ставить на дирижабль"), true);
			return;
		}
		if (this.placed.size() >= ModConfig.maxBlocks) {
			p.sendStatusMessage(new TextComponentString("Лимит блоков: " + ModConfig.maxBlocks), true);
			return;
		}
		Vec3d eye = p.getPositionEyes(1.0F);
		Vec3d hit = new Vec3d(this.posX + vec.x, this.posY + vec.y, this.posZ + vec.z);
		Vec3d dir = hit.subtract(eye);
		if (dir.lengthSquared() < 1.0E-6) {
			return;
		}
		dir = dir.normalize();
		Vec3d q = hit.subtract(dir.scale(0.02));
		Vec3d loc = this.toLocal(q.x - this.posX, q.z - this.posZ, q.y - this.posY);
		BlockPos cell = new BlockPos(MathHelper.floor(loc.x), MathHelper.floor(loc.y), MathHelper.floor(loc.z));
		if (!AirshipLayout.inBuildArea(cell) || AirshipLayout.isHull(cell) || this.cellAt(cell) != null) {
			p.sendStatusMessage(new TextComponentString("Сюда поставить нельзя"), true);
			return;
		}
		this.placed.add(new Cell(cell, s));
		if (!p.isCreative()) {
			st.shrink(1);
		}
		this.world.playSound(null, this.posX, this.posY, this.posZ, s.getBlock().getSoundType().getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 0.8F);
		this.recalcMass();
		this.syncBlocks();
	}

	private void removeLast(EntityPlayer p) {
		if (this.placed.isEmpty()) {
			p.sendStatusMessage(new TextComponentString("На дирижабле нет поставленных блоков"), true);
			return;
		}
		Cell c = this.placed.remove(this.placed.size() - 1);
		ItemStack drop = new ItemStack(Item.getItemFromBlock(c.state.getBlock()), 1, c.state.getBlock().damageDropped(c.state));
		if (!p.addItemStackToInventory(drop)) {
			p.dropItem(drop, false);
		}
		this.recalcMass();
		this.syncBlocks();
	}

	public Cell cellAt(BlockPos p) {
		for (Cell c : this.placed) {
			if (c.pos.equals(p)) {
				return c;
			}
		}
		return null;
	}

	public void clearPlaced(boolean dropItems) {
		if (dropItems) {
			for (Cell c : this.placed) {
				this.entityDropItem(new ItemStack(Item.getItemFromBlock(c.state.getBlock()), 1, c.state.getBlock().damageDropped(c.state)), 0.5F);
			}
		}
		this.placed.clear();
		this.recalcMass();
		this.syncBlocks();
	}

	private void recalcMass() {
		double sx = 0, sz = 0;
		for (Cell c : this.placed) {
			sx += c.pos.getX() + 0.5;
			sz += c.pos.getZ() + 0.5;
		}
		double k = this.placed.size() + 12.0; // 12 - вес корпуса
		this.comX = sx / k;
		this.comZ = sz / k;
	}

	public void syncBlocks() {
		if (!this.world.isRemote) {
			JungleAirshipMod.network.sendToAllAround(new MessageBlocks(this),
					new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 200.0));
		}
	}

	public void setPlacedFromNetwork(List<Cell> cells) {
		this.placed.clear();
		this.placed.addAll(cells);
		this.recalcMass();
	}

	@Override
	public boolean attackEntityFrom(DamageSource src, float amount) {
		if (this.world.isRemote || this.isDead) {
			return true;
		}
		Entity s = src.getTrueSource();
		if (s instanceof EntityPlayer) {
			EntityPlayer p = (EntityPlayer) s;
			if (ModAccess.isAdmin(p) && p.isSneaking()) {
				this.clearPlaced(true);
				this.setDead();
			}
		}
		return true;
	}

	// ---------- сохранение ----------
	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
		this.setFuel(nbt.getInteger("fuel"));
		this.placed.clear();
		NBTTagList l = nbt.getTagList("placed", 10);
		for (int i = 0; i < l.tagCount(); i++) {
			NBTTagCompound t = l.getCompoundTagAt(i);
			Block b = Block.REGISTRY.getObject(new ResourceLocation(t.getString("id")));
			if (b != null && b != net.minecraft.init.Blocks.AIR) {
				IBlockState s = b.getStateFromMeta(t.getInteger("m"));
				this.placed.add(new Cell(new BlockPos(t.getInteger("x"), t.getInteger("y"), t.getInteger("z")), s));
			}
		}
		this.recalcMass();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		nbt.setInteger("fuel", this.getFuel());
		NBTTagList l = new NBTTagList();
		for (Cell c : this.placed) {
			NBTTagCompound t = new NBTTagCompound();
			t.setInteger("x", c.pos.getX());
			t.setInteger("y", c.pos.getY());
			t.setInteger("z", c.pos.getZ());
			t.setString("id", c.state.getBlock().getRegistryName().toString());
			t.setInteger("m", c.state.getBlock().getMetaFromState(c.state));
			l.appendTag(t);
		}
		nbt.setTag("placed", l);
	}

	// ---------- сеть (общие функции для спавна и пакета) ----------
	public static void writeCells(ByteBuf buf, List<Cell> cells) {
		buf.writeInt(cells.size());
		for (Cell c : cells) {
			buf.writeInt(c.pos.getX());
			buf.writeInt(c.pos.getY());
			buf.writeInt(c.pos.getZ());
			buf.writeInt(Block.getStateId(c.state));
		}
	}

	public static List<Cell> readCells(ByteBuf buf) {
		int n = buf.readInt();
		List<Cell> l = new ArrayList<Cell>();
		for (int i = 0; i < n; i++) {
			BlockPos p = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
			IBlockState s = Block.getStateById(buf.readInt());
			l.add(new Cell(p, s));
		}
		return l;
	}

	@Override
	public void writeSpawnData(ByteBuf buf) {
		writeCells(buf, this.placed);
	}

	@Override
	public void readSpawnData(ByteBuf buf) {
		this.setPlacedFromNetwork(readCells(buf));
	}
}
