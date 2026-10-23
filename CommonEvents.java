package kirill.jungleairship;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = JungleAirshipMod.MODID, name = "Jungle Airship", version = "1.0.0", acceptedMinecraftVersions = "[1.12.2]")
public class JungleAirshipMod {
	public static final String MODID = "jungleairship";

	@Mod.Instance(MODID)
	public static JungleAirshipMod instance;

	@SidedProxy(clientSide = "kirill.jungleairship.client.ClientProxy", serverSide = "kirill.jungleairship.CommonProxy")
	public static CommonProxy proxy;

	public static SimpleNetworkWrapper network;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		ModConfig.load(e.getSuggestedConfigurationFile());
		EntityRegistry.registerModEntity(new ResourceLocation(MODID, "airship"), EntityAirship.class, "airship", 0, this, 128, 1, true);
		network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
		network.registerMessage(MessageControls.Handler.class, MessageControls.class, 0, Side.SERVER);
		network.registerMessage(MessageBlocks.Handler.class, MessageBlocks.class, 1, Side.CLIENT);
		MinecraftForge.EVENT_BUS.register(new CommonEvents());
		MinecraftForge.TERRAIN_GEN_BUS.register(new BiomeForcer());
		proxy.preInit();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent e) {
		GameRegistry.registerWorldGenerator(new JungleWorldGenerator(), 3);
	}

	@Mod.EventHandler
	public void serverStarting(FMLServerStartingEvent e) {
		e.registerServerCommand(new CommandAirship());
	}
}
