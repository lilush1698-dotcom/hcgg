package kirill.jungleairship;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class CommonEvents {
	@SubscribeEvent
	public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
		if (e.player.world.isRemote || !(e.player instanceof EntityPlayerMP)) {
			return;
		}
		MinecraftServer srv = FMLCommonHandler.instance().getMinecraftServerInstance();
		WorldServer w = srv.getWorld(0);
		AirshipData d = AirshipData.get(w);
		if (!d.spawned && ModConfig.autoSpawn) {
			d.spawned = true;
			d.markDirty();
			SpawnSetup.createStart(w, (EntityPlayerMP) e.player);
		}
		if (ModAccess.isDisabled(e.player)) {
			e.player.sendMessage(new TextComponentString("[Jungle Airship] Мод отключён для тебя администратором."));
		} else if (ModAccess.isAdmin(e.player)) {
			e.player.sendMessage(new TextComponentString("[Jungle Airship] Админ-режим включён. Список команд: /airship help"));
		}
	}
}
