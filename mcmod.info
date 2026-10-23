package kirill.jungleairship.client;

import kirill.jungleairship.CommonProxy;
import kirill.jungleairship.EntityAirship;
import kirill.jungleairship.MessageBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	@Override
	public void preInit() {
		RenderingRegistry.registerEntityRenderingHandler(EntityAirship.class, new net.minecraftforge.fml.client.registry.IRenderFactory<EntityAirship>() {
			@Override
			public net.minecraft.client.renderer.entity.Render<? super EntityAirship> createRenderFor(net.minecraft.client.renderer.entity.RenderManager manager) {
				return new RenderAirship(manager);
			}
		});
		MinecraftForge.EVENT_BUS.register(new ClientEvents());
	}

	@Override
	public void handleBlocks(final MessageBlocks msg) {
		final Minecraft mc = Minecraft.getMinecraft();
		mc.addScheduledTask(new Runnable() {
			@Override
			public void run() {
				if (mc.world == null) {
					return;
				}
				Entity e = mc.world.getEntityByID(msg.entityId);
				if (e instanceof EntityAirship) {
					((EntityAirship) e).setPlacedFromNetwork(msg.cells);
				}
			}
		});
	}

	@Override
	public EntityPlayer getLocalPlayer() {
		return Minecraft.getMinecraft().player;
	}
}
