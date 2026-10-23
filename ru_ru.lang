package kirill.jungleairship.client;

import kirill.jungleairship.EntityAirship;
import kirill.jungleairship.JungleAirshipMod;
import kirill.jungleairship.MessageControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Пока игрок сидит на дирижабле, отправляет серверу нажатые клавиши (W/S/A/D, Space, Ctrl). */
@SideOnly(Side.CLIENT)
public class ClientEvents {
	private int last = -1;
	private int tick;

	@SubscribeEvent
	public void onTick(TickEvent.ClientTickEvent e) {
		if (e.phase != TickEvent.Phase.END) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP p = mc.player;
		if (p == null || !(p.getRidingEntity() instanceof EntityAirship)) {
			last = -1;
			return;
		}
		int f = 0;
		if (mc.currentScreen == null) {
			GameSettings gs = mc.gameSettings;
			if (gs.keyBindForward.isKeyDown()) {
				f |= 1;
			}
			if (gs.keyBindBack.isKeyDown()) {
				f |= 2;
			}
			if (gs.keyBindLeft.isKeyDown()) {
				f |= 4;
			}
			if (gs.keyBindRight.isKeyDown()) {
				f |= 8;
			}
			if (gs.keyBindJump.isKeyDown()) {
				f |= 16;
			}
			if (gs.keyBindSprint.isKeyDown()) {
				f |= 32;
			}
		}
		tick++;
		if (f != last || tick % 10 == 0) {
			JungleAirshipMod.network.sendToServer(new MessageControls(f));
			last = f;
		}
	}
}
