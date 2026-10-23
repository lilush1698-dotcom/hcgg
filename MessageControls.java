package kirill.jungleairship;

import java.util.Random;

import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

/** Ямы/разломы как на карте из Roblox и редкие области (болото, кристаллы, кости, лёд). */
public class JungleWorldGenerator implements IWorldGenerator {
	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator gen, IChunkProvider prov) {
		if (world.provider.getDimension() != 0) {
			return;
		}
		int bx = chunkX * 16 + 8;
		int bz = chunkZ * 16 + 8;
		BlockPos spawn = world.getSpawnPoint();
		double dx = bx - spawn.getX();
		double dz = bz - spawn.getZ();
		if (dx * dx + dz * dz < 110.0 * 110.0) {
			return;
		}
		if (ModConfig.genRare) {
			rare(random, chunkX, chunkZ, world, bx, bz, spawn);
		}
		if (ModConfig.genChasms) {
			chasm(random, world, bx, bz);
		}
	}

	private void chasm(Random r, World w, int bx, int bz) {
		if (r.nextInt(12) != 0) {
			return;
		}
		int x = bx + 3 + r.nextInt(10);
		int z = bz + 3 + r.nextInt(10);
		double ang = r.nextDouble() * Math.PI * 2;
		int steps = 4 + r.nextInt(10);
		for (int s = 0; s < steps; s++) {
			carve(w, r, x, z, 2 + r.nextInt(3));
			ang += (r.nextDouble() - 0.5) * 1.2;
			x = clamp(x + (int) Math.round(Math.cos(ang) * 3), bx + 3, bx + 12);
			z = clamp(z + (int) Math.round(Math.sin(ang) * 3), bz + 3, bz + 12);
		}
	}

	private void carve(World w, Random r, int cx, int cz, int rad) {
		for (int ox = -rad; ox <= rad; ox++) {
			for (int oz = -rad; oz <= rad; oz++) {
				if (ox * ox + oz * oz > rad * rad) {
					continue;
				}
				int x = cx + ox;
				int z = cz + oz;
				int top = Math.min(250, w.getHeight(x, z));
				int y0 = Math.max(8, SpawnSetup.groundY(w, x, z) - 24);
				for (int y = top; y >= y0; y--) {
					BlockPos p = new BlockPos(x, y, z);
					if (w.getBlockState(p).getBlock() != Blocks.BEDROCK) {
						w.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
					}
				}
				if (r.nextInt(3) == 0) {
					w.setBlockState(new BlockPos(x, y0 - 1, z), Blocks.MOSSY_COBBLESTONE.getDefaultState(), 2);
				}
			}
		}
	}

	private void rare(Random random, int chunkX, int chunkZ, World w, int bx, int bz, BlockPos spawn) {
		int rx = Math.floorDiv(chunkX, 6);
		int rz = Math.floorDiv(chunkZ, 6);
		Random rr = new Random(w.getSeed() * 31L + rx * 341873128712L + rz * 132897987541L + 7L);
		if (rr.nextFloat() >= 0.06f) {
			return;
		}
		double cx = (rx * 6 + 3) * 16.0 - spawn.getX();
		double cz = (rz * 6 + 3) * 16.0 - spawn.getZ();
		if (cx * cx + cz * cz < 200.0 * 200.0) {
			return;
		}
		int type = rr.nextInt(4);
		IBlockState surface;
		switch (type) {
		case 0:
			surface = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL);
			break;
		case 1:
			surface = Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE_SMOOTH);
			break;
		case 2:
			surface = Blocks.SAND.getDefaultState();
			break;
		default:
			surface = Blocks.SNOW.getDefaultState();
			break;
		}
		for (int ox = 0; ox < 16; ox++) {
			for (int oz = 0; oz < 16; oz++) {
				int x = bx + ox;
				int z = bz + oz;
				int y = SpawnSetup.groundY(w, x, z);
				if (y < 0) {
					continue;
				}
				if (type == 1 || type == 2) {
					for (int yy = y + 1; yy <= y + 30; yy++) {
						BlockPos p = new BlockPos(x, yy, z);
						if (!w.isAirBlock(p)) {
							w.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
						}
					}
				}
				w.setBlockState(new BlockPos(x, y, z), surface, 2);
			}
		}
		int count = 3 + random.nextInt(4);
		for (int i = 0; i < count; i++) {
			int x = bx + 3 + random.nextInt(10);
			int z = bz + 3 + random.nextInt(10);
			int y = SpawnSetup.groundY(w, x, z);
			if (y < 0) {
				continue;
			}
			switch (type) {
			case 0:
				pool(w, random, x, y, z);
				break;
			case 1:
				crystal(w, random, x, y + 1, z);
				break;
			case 2:
				bones(w, random, x, y + 1, z);
				break;
			default:
				iceSpike(w, random, x, y + 1, z);
				break;
			}
		}
	}

	private void pool(World w, Random r, int cx, int y, int cz) {
		int rad = 2 + r.nextInt(2);
		for (int ox = -rad; ox <= rad; ox++) {
			for (int oz = -rad; oz <= rad; oz++) {
				if (ox * ox + oz * oz > rad * rad) {
					continue;
				}
				BlockPos p = new BlockPos(cx + ox, y, cz + oz);
				w.setBlockState(p, Blocks.WATER.getDefaultState(), 2);
				w.setBlockState(p.up(), Blocks.AIR.getDefaultState(), 2);
				if (r.nextInt(4) == 0) {
					w.setBlockState(p.up(), Blocks.WATERLILY.getDefaultState(), 2);
				}
			}
		}
	}

	private void crystal(World w, Random r, int x, int y, int z) {
		EnumDyeColor[] cols = { EnumDyeColor.CYAN, EnumDyeColor.LIGHT_BLUE, EnumDyeColor.PURPLE, EnumDyeColor.MAGENTA };
		IBlockState glass = Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, cols[r.nextInt(cols.length)]);
		int h = 4 + r.nextInt(7);
		w.setBlockState(new BlockPos(x, y, z), Blocks.SEA_LANTERN.getDefaultState(), 2);
		for (int i = 1; i < h; i++) {
			w.setBlockState(new BlockPos(x, y + i, z), glass, 2);
		}
		int[][] side = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
		for (int[] s : side) {
			int hh = 1 + r.nextInt(Math.max(1, h / 2));
			for (int i = 0; i < hh; i++) {
				w.setBlockState(new BlockPos(x + s[0], y + i, z + s[1]), glass, 2);
			}
		}
	}

	private void bones(World w, Random r, int x, int y, int z) {
		int h = 3 + r.nextInt(5);
		for (int i = 0; i < h; i++) {
			w.setBlockState(new BlockPos(x, y + i, z), Blocks.BONE_BLOCK.getDefaultState(), 2);
		}
		for (int i = -2; i <= 2; i++) {
			int hh = 3 - Math.abs(i);
			for (int j = 0; j < hh; j++) {
				w.setBlockState(new BlockPos(x + i, y + j, z + 3), Blocks.BONE_BLOCK.getDefaultState(), 2);
			}
		}
		w.setBlockState(new BlockPos(x + 2, y, z - 2), Blocks.COBBLESTONE_WALL.getDefaultState(), 2);
		w.setBlockState(new BlockPos(x + 2, y + 1, z - 2), Blocks.STONE_SLAB.getDefaultState(), 2);
	}

	private void iceSpike(World w, Random r, int x, int y, int z) {
		int h = 3 + r.nextInt(7);
		for (int i = 0; i < h; i++) {
			int rad = (i < h / 3) ? 1 : 0;
			for (int ox = -rad; ox <= rad; ox++) {
				for (int oz = -rad; oz <= rad; oz++) {
					w.setBlockState(new BlockPos(x + ox, y + i, z + oz), Blocks.PACKED_ICE.getDefaultState(), 2);
				}
			}
		}
	}

	private static int clamp(int v, int lo, int hi) {
		return Math.max(lo, Math.min(hi, v));
	}
}
