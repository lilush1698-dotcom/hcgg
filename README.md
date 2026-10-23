package kirill.jungleairship;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Расчищает поляну у спавна мира и ставит туда дирижабль, уголь и топор. */
public class SpawnSetup {
	public static int groundY(World w, int x, int z) {
		int top = Math.min(250, w.getHeight(x, z));
		for (int y = top; y > 1; y--) {
			IBlockState s = w.getBlockState(new BlockPos(x, y, z));
			Material m = s.getMaterial();
			if (m == Material.GROUND || m == Material.GRASS || m == Material.ROCK || m == Material.SAND || m == Material.CLAY || m == Material.SNOW
					|| m == Material.CRAFTED_SNOW || m == Material.ICE || m == Material.PACK_ICE) {
				return y;
			}
		}
		return -1;
	}

	public static void createStart(WorldServer w, EntityPlayerMP player) {
		BlockPos sp = w.getSpawnPoint();
		int cx = sp.getX();
		int cz = sp.getZ();
		w.getChunkFromBlockCoords(new BlockPos(cx, 64, cz));
		int gy = groundY(w, cx, cz);
		if (gy < 0) {
			gy = Math.max(sp.getY(), 64);
		}
		int radius = 11;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) {
					continue;
				}
				int x = cx + dx;
				int z = cz + dz;
				int h = groundY(w, x, z);
				for (int y = Math.max(h, 1) + 1; y < gy; y++) {
					w.setBlockState(new BlockPos(x, y, z), Blocks.DIRT.getDefaultState(), 2);
				}
				w.setBlockState(new BlockPos(x, gy, z), Blocks.GRASS.getDefaultState(), 2);
				for (int y = gy + 1; y <= gy + 20; y++) {
					BlockPos p = new BlockPos(x, y, z);
					if (!w.isAirBlock(p)) {
						w.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
					}
				}
			}
		}
		EntityAirship ship = new EntityAirship(w);
		ship.setLocationAndAngles(cx + 0.5, gy + 1.0, cz + 0.5, 0.0F, 0.0F);
		w.spawnEntity(ship);

		drop(w, new ItemStack(Items.COAL, 16), cx - 7.5, gy + 1, cz + 3.5);
		drop(w, new ItemStack(Items.COAL, 1, 1), cx - 7.5, gy + 1, cz + 4.5);
		drop(w, new ItemStack(Items.IRON_AXE), cx - 8.5, gy + 1, cz + 3.5);
		drop(w, new ItemStack(Items.TORCH, 16), cx - 8.5, gy + 1, cz + 4.5);

		player.setPositionAndUpdate(cx - 7.0, gy + 1.0, cz + 0.5);
		w.setSpawnPoint(new BlockPos(cx - 7, gy + 1, cz));
	}

	private static void drop(World w, ItemStack st, double x, double y, double z) {
		w.spawnEntity(new EntityItem(w, x, y, z, st));
	}
}
