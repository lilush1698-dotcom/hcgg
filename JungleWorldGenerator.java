package kirill.jungleairship;

import net.minecraft.entity.player.EntityPlayer;

public class ModAccess {
	public static boolean isAdmin(EntityPlayer p) {
		String n = p.getName();
		for (String a : ModConfig.admins) {
			if (a.equalsIgnoreCase(n)) {
				return true;
			}
		}
		String u = p.getUniqueID().toString();
		for (String a : ModConfig.adminUuids) {
			if (a.equalsIgnoreCase(u)) {
				return true;
			}
		}
		return false;
	}

	/** Мод отключён для игрока (навсегда, пока админ не включит обратно). Админы не отключаются. */
	public static boolean isDisabled(EntityPlayer p) {
		if (isAdmin(p)) {
			return false;
		}
		String n = p.getName();
		for (String d : ModConfig.disabled) {
			if (d.equalsIgnoreCase(n)) {
				return true;
			}
		}
		return AirshipData.get(p.world).disabled.contains(p.getUniqueID());
	}
}
