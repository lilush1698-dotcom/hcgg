package kirill.jungleairship;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class ModConfig {
	public static Configuration cfg;
	public static String[] admins;
	public static String[] adminUuids;
	public static String[] disabled;
	public static boolean forceJungle;
	public static boolean genChasms;
	public static boolean genRare;
	public static boolean autoSpawn;
	public static int maxBlocks;

	public static void load(File f) {
		cfg = new Configuration(f);
		admins = cfg.getStringList("admins", "access", new String[] { "SuperKirill2020" },
				"Ники админов мода (регистр не важен). На серверах в offline-режиме ник можно подделать - добавь UUID в adminUuids.");
		adminUuids = cfg.getStringList("adminUuids", "access", new String[0], "UUID админов (надёжнее ника).");
		disabled = cfg.getStringList("disabledPlayers", "access", new String[0],
				"Игроки, у которых мод отключён навсегда (ники). Меняется командами /airship disable|enable.");
		forceJungle = cfg.getBoolean("forceJungle", "world", true, "Весь мир - джунгли (как карта в Roblox).");
		genChasms = cfg.getBoolean("genChasms", "world", true, "Генерировать ямы и разломы.");
		genRare = cfg.getBoolean("genRareBiomes", "world", true, "Редкие области: болото, кристаллы, кости, лёд.");
		autoSpawn = cfg.getBoolean("autoSpawnAirship", "world", true, "Ставить дирижабль на поляне у спавна в новом мире.");
		maxBlocks = cfg.getInt("maxPlacedBlocks", "airship", 300, 20, 2000, "Максимум блоков, которые можно поставить на дирижабль.");
		if (cfg.hasChanged()) {
			cfg.save();
		}
	}

	public static void setDisabledNames(List<String> names) {
		Property p = cfg.get("access", "disabledPlayers", new String[0]);
		disabled = names.toArray(new String[0]);
		p.set(disabled);
		cfg.save();
	}

	public static void addDisabled(String name) {
		List<String> l = new ArrayList<String>();
		for (String s : disabled) {
			l.add(s);
		}
		for (String s : l) {
			if (s.equalsIgnoreCase(name)) {
				return;
			}
		}
		l.add(name);
		setDisabledNames(l);
	}

	public static void removeDisabled(String name) {
		List<String> l = new ArrayList<String>();
		for (String s : disabled) {
			if (!s.equalsIgnoreCase(name)) {
				l.add(s);
			}
		}
		setDisabledNames(l);
	}
}
