package kirill.jungleairship;

import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;

/** Слой биомов: везде джунгли. */
public class GenLayerJungle extends GenLayer {
	public GenLayerJungle(long seed) {
		super(seed);
	}

	@Override
	public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
		int[] r = IntCache.getIntCache(areaWidth * areaHeight);
		int id = Biome.getIdForBiome(Biomes.JUNGLE);
		for (int i = 0; i < r.length; i++) {
			r[i] = id;
		}
		return r;
	}
}
