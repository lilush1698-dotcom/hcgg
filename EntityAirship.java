package kirill.jungleairship;

import net.minecraft.world.WorldType;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Делает каждый новый обычный мир джунглями. Отключается в конфиге (forceJungle=false). */
public class BiomeForcer {
	@SubscribeEvent
	public void onInit(WorldTypeEvent.InitBiomeGens e) {
		if (!ModConfig.forceJungle) {
			return;
		}
		WorldType t = e.getWorldType();
		if (t != WorldType.DEFAULT && t != WorldType.LARGE_BIOMES && t != WorldType.AMPLIFIED) {
			return;
		}
		GenLayer jungle = new GenLayerJungle(1L);
		e.setNewBiomeGens(new GenLayer[] { jungle, jungle, jungle });
	}
}
