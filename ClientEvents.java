package kirill.jungleairship;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;

/** Готовый корпус дирижабля (палуба, перила, канаты, печка и шар из шерсти). Координаты - локальные клетки корабля. */
public final class AirshipLayout {
	// область, куда игрок может ставить блоки (локальные клетки)
	public static final int MINX = -5, MAXX = 4, MINY = -3, MAXY = 3, MINZ = -5, MAXZ = 4;

	private static List<EntityAirship.Cell> hull;
	private static Set<Long> occupied;

	public static synchronized List<EntityAirship.Cell> hull() {
		if (hull == null) {
			build();
		}
		return hull;
	}

	public static synchronized boolean isHull(BlockPos p) {
		hull();
		return occupied.contains(p.toLong());
	}

	public static boolean inBuildArea(BlockPos p) {
		return p.getX() >= MINX && p.getX() <= MAXX && p.getY() >= MINY && p.getY() <= MAXY && p.getZ() >= MINZ && p.getZ() <= MAXZ;
	}

	private static void add(List<EntityAirship.Cell> l, Set<Long> occ, int x, int y, int z, IBlockState s) {
		BlockPos p = new BlockPos(x, y, z);
		if (occ.add(p.toLong())) {
			l.add(new EntityAirship.Cell(p, s));
		}
	}

	private static void build() {
		List<EntityAirship.Cell> l = new ArrayList<EntityAirship.Cell>();
		Set<Long> occ = new HashSet<Long>();
		IBlockState planks = Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.SPRUCE);
		IBlockState darkPlanks = Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.DARK_OAK);
		IBlockState fence = Blocks.OAK_FENCE.getDefaultState();

		// палуба 6x6
		for (int x = -3; x <= 2; x++) {
			for (int z = -3; z <= 2; z++) {
				boolean edge = x == -3 || x == 2 || z == -3 || z == 2;
				add(l, occ, x, 0, z, edge ? darkPlanks : planks);
			}
		}
		// перила
		for (int x = -3; x <= 2; x++) {
			for (int z = -3; z <= 2; z++) {
				if (x == -3 || x == 2 || z == -3 || z == 2) {
					add(l, occ, x, 1, z, fence);
				}
			}
		}
		// канаты к шару
		int[][] ropes = { { -2, -2 }, { 1, -2 }, { -2, 1 }, { 1, 1 } };
		for (int[] r : ropes) {
			for (int y = 1; y <= 4; y++) {
				add(l, occ, r[0], y, r[1], fence);
			}
		}
		// печка
		add(l, occ, -2, 1, 0, Blocks.FURNACE.getDefaultState());
		// шар (оболочка эллипсоида из шерсти)
		for (int x = -6; x <= 5; x++) {
			for (int y = 4; y <= 14; y++) {
				for (int z = -9; z <= 8; z++) {
					double dx = (x + 0.5 - 0.0) / 5.4;
					double dy = (y + 0.5 - 9.5) / 5.0;
					double dz = (z + 0.5 - 0.0) / 8.6;
					double d2 = dx * dx + dy * dy + dz * dz;
					if (d2 <= 1.0 && d2 > 0.66) {
						int band = Math.floorMod((z + 9) / 3, 2);
						EnumDyeColor c = band == 0 ? EnumDyeColor.RED : EnumDyeColor.WHITE;
						if (y == 4 || y == 5) {
							c = EnumDyeColor.ORANGE;
						}
						add(l, occ, x, y, z, Blocks.WOOL.getDefaultState().withProperty(BlockColored.COLOR, c));
					}
				}
			}
		}
		occupied = occ;
		hull = l;
	}

	private AirshipLayout() {
	}
}
