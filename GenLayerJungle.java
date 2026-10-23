package kirill.jungleairship;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

public class AirshipData extends WorldSavedData {
	public static final String NAME = "jungleairship_data";
	public boolean spawned;
	public Set<UUID> disabled = new HashSet<UUID>();

	public AirshipData() {
		super(NAME);
	}

	public AirshipData(String name) {
		super(name);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		spawned = nbt.getBoolean("spawned");
		disabled.clear();
		NBTTagList l = nbt.getTagList("disabled", 8);
		for (int i = 0; i < l.tagCount(); i++) {
			try {
				disabled.add(UUID.fromString(l.getStringTagAt(i)));
			} catch (IllegalArgumentException ex) {
				// пропускаем битую запись
			}
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setBoolean("spawned", spawned);
		NBTTagList l = new NBTTagList();
		for (UUID u : disabled) {
			l.appendTag(new NBTTagString(u.toString()));
		}
		nbt.setTag("disabled", l);
		return nbt;
	}

	public static AirshipData get(World w) {
		MapStorage s = w.getMapStorage();
		AirshipData d = (AirshipData) s.getOrLoadData(AirshipData.class, NAME);
		if (d == null) {
			d = new AirshipData();
			s.setData(NAME, d);
		}
		return d;
	}
}
