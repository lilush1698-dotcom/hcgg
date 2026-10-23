package kirill.jungleairship;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** /airship ... - админские команды мода. Работают только у админов (см. config) и из консоли сервера. */
public class CommandAirship extends CommandBase {
	private static final String USAGE = "/airship <help|status|spawn|remove|fuel N|clearblocks|tpship|goship|give coal|disable <ник>|enable <ник>|fly|heal|time day|night|weather clear|rain>";

	@Override
	public String getName() {
		return "airship";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return USAGE;
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		if (sender instanceof EntityPlayer) {
			return ModAccess.isAdmin((EntityPlayer) sender);
		}
		return true; // консоль / командный блок
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, Arrays.asList("help", "status", "spawn", "remove", "fuel", "clearblocks", "tpship", "goship",
					"give", "disable", "enable", "fly", "heal", "time", "weather"));
		}
		if (args.length == 2 && (args[0].equals("disable") || args[0].equals("enable"))) {
			return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
		}
		return java.util.Collections.<String>emptyList();
	}

	private static void msg(ICommandSender s, String t) {
		s.sendMessage(new TextComponentString("[Jungle Airship] " + t));
	}

	private static EntityAirship nearest(World w, double x, double y, double z) {
		EntityAirship best = null;
		double bd = Double.MAX_VALUE;
		for (Entity e : w.loadedEntityList) {
			if (e instanceof EntityAirship) {
				double d = e.getDistanceSq(x, y, z);
				if (d < bd) {
					bd = d;
					best = (EntityAirship) e;
				}
			}
		}
		return best;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0 || args[0].equals("help")) {
			msg(sender, USAGE);
			return;
		}
		String sub = args[0];
		if (sub.equals("disable") || sub.equals("enable")) {
			if (args.length < 2) {
				throw new WrongUsageException("/airship " + sub + " <ник>");
			}
			setDisabled(server, sender, args[1], sub.equals("disable"));
			return;
		}
		if (sub.equals("status")) {
			WorldServer w0 = server.getWorld(0);
			int ships = 0;
			for (Entity e : w0.loadedEntityList) {
				if (e instanceof EntityAirship) {
					ships++;
				}
			}
			msg(sender, "дирижаблей в загруженных чанках: " + ships + "; отключены: " + AirshipData.get(w0).disabled.size() + " UUID + "
					+ ModConfig.disabled.length + " ников; админы: " + Arrays.toString(ModConfig.admins));
			return;
		}
		EntityPlayerMP p = getCommandSenderAsPlayer(sender);
		World w = p.world;
		if (sub.equals("spawn")) {
			EntityAirship ship = new EntityAirship(w);
			ship.setLocationAndAngles(p.posX, p.posY + 1.0, p.posZ + 6.0, 0.0F, 0.0F);
			ship.setFuel(EntityAirship.MAX_FUEL / 2);
			w.spawnEntity(ship);
			msg(sender, "дирижабль создан (топливо 50%)");
			return;
		}
		if (sub.equals("fly")) {
			boolean on = !p.capabilities.allowFlying;
			p.capabilities.allowFlying = on;
			if (!on) {
				p.capabilities.isFlying = false;
			}
			p.sendPlayerAbilities();
			msg(sender, "полёт " + (on ? "включён" : "выключен"));
			return;
		}
		if (sub.equals("heal")) {
			p.setHealth(p.getMaxHealth());
			p.getFoodStats().addStats(20, 1.0F);
			msg(sender, "здоровье и сытость восстановлены");
			return;
		}
		if (sub.equals("time") && args.length > 1) {
			for (WorldServer ws : server.worlds) {
				ws.setWorldTime(args[1].equals("night") ? 14000L : 1000L);
			}
			msg(sender, "время: " + args[1]);
			return;
		}
		if (sub.equals("weather") && args.length > 1) {
			WorldServer ws = server.getWorld(0);
			boolean rain = !args[1].equals("clear");
			ws.getWorldInfo().setRaining(rain);
			ws.getWorldInfo().setThundering(args[1].equals("storm"));
			ws.getWorldInfo().setRainTime(rain ? 6000 : 0);
			msg(sender, "погода: " + args[1]);
			return;
		}
		if (sub.equals("give")) {
			p.addItemStackToInventory(new ItemStack(Items.COAL, 32));
			msg(sender, "выдан уголь x32");
			return;
		}
		EntityAirship ship = nearest(w, p.posX, p.posY, p.posZ);
		if (ship == null) {
			throw new CommandException("Дирижабль не найден в загруженных чанках");
		}
		if (sub.equals("remove")) {
			ship.clearPlaced(true);
			ship.setDead();
			msg(sender, "дирижабль удалён");
		} else if (sub.equals("fuel")) {
			int pct = args.length > 1 ? parseInt(args[1], 0, 100) : 100;
			ship.setFuel((int) (EntityAirship.MAX_FUEL * pct / 100.0));
			msg(sender, "топливо: " + pct + "%");
		} else if (sub.equals("clearblocks")) {
			ship.clearPlaced(true);
			msg(sender, "блоки сняты с дирижабля");
		} else if (sub.equals("tpship")) {
			ship.setPositionAndUpdate(p.posX, p.posY + 1.0, p.posZ + 6.0);
			ship.motionX = ship.motionY = ship.motionZ = 0.0;
			msg(sender, "дирижабль перенесён к тебе");
		} else if (sub.equals("goship")) {
			p.setPositionAndUpdate(ship.posX, ship.posY + 1.5, ship.posZ);
			msg(sender, "ты на дирижабле");
		} else {
			throw new WrongUsageException(USAGE);
		}
	}

	/** Навсегда отключает / включает мод игроку: пишется и в конфиг (по нику), и в данные мира (по UUID). */
	private void setDisabled(MinecraftServer server, ICommandSender sender, String name, boolean off) {
		EntityPlayerMP online = server.getPlayerList().getPlayerByUsername(name);
		AirshipData d = AirshipData.get(server.getWorld(0));
		if (online != null && ModAccess.isAdmin(online)) {
			msg(sender, "администратора отключить нельзя");
			return;
		}
		if (off) {
			ModConfig.addDisabled(name);
			if (online != null) {
				d.disabled.add(online.getUniqueID());
				if (online.getRidingEntity() instanceof EntityAirship) {
					online.dismountRidingEntity();
				}
				online.sendMessage(new TextComponentString("[Jungle Airship] Мод отключён для тебя администратором."));
			}
			d.markDirty();
			msg(sender, "мод отключён навсегда для: " + name + " (команда /airship enable " + name + " вернёт доступ)");
		} else {
			ModConfig.removeDisabled(name);
			if (online != null) {
				d.disabled.remove(online.getUniqueID());
			}
			d.markDirty();
			msg(sender, "мод снова включён для: " + name);
		}
	}
}
