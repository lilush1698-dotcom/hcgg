package kirill.jungleairship.client;

import java.util.ArrayList;
import java.util.List;

import kirill.jungleairship.AirshipLayout;
import kirill.jungleairship.EntityAirship;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Рисует корпус (шар, палуба, перила) и поставленные блоки как обычные блоки Minecraft. */
@SideOnly(Side.CLIENT)
public class RenderAirship extends Render<EntityAirship> {
	public RenderAirship(RenderManager rm) {
		super(rm);
		this.shadowSize = 0.0F;
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityAirship e) {
		return TextureMap.LOCATION_BLOCKS_TEXTURE;
	}

	@Override
	public void doRender(EntityAirship e, double x, double y, double z, float yaw, float pt) {
		GlStateManager.pushMatrix();
		GlStateManager.translate((float) x, (float) y, (float) z);
		GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
		float roll = e.prevRoll + (e.getRoll() - e.prevRoll) * pt;
		float pitch = e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * pt;
		GlStateManager.translate(0.0F, 4.0F, 0.0F);
		GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
		GlStateManager.translate(0.0F, -4.0F, 0.0F);
		this.bindEntityTexture(e);
		GlStateManager.disableLighting();

		Tessellator t = Tessellator.getInstance();
		BufferBuilder buf = t.getBuffer();
		buf.begin(7, DefaultVertexFormats.BLOCK);
		BlockPos bp = new BlockPos(e.posX, e.posY + 3.0, e.posZ);
		BlockRendererDispatcher d = Minecraft.getMinecraft().getBlockRendererDispatcher();
		draw(d, buf, e, bp, AirshipLayout.hull());
		draw(d, buf, e, bp, new ArrayList<EntityAirship.Cell>(e.placed));
		buf.setTranslation(0.0, 0.0, 0.0);
		t.draw();

		GlStateManager.enableLighting();
		GlStateManager.popMatrix();
		super.doRender(e, x, y, z, yaw, pt);
	}

	private void draw(BlockRendererDispatcher d, BufferBuilder buf, EntityAirship e, BlockPos bp, List<EntityAirship.Cell> cells) {
		for (EntityAirship.Cell c : cells) {
			IBlockState s = c.state;
			IBakedModel m = d.getModelForState(s);
			buf.setTranslation(c.pos.getX() - bp.getX(), c.pos.getY() - bp.getY(), c.pos.getZ() - bp.getZ());
			d.getBlockModelRenderer().renderModel(e.world, m, s, bp, buf, false);
		}
	}
}
