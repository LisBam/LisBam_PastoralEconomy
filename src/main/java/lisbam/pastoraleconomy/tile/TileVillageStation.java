package lisbam.pastoraleconomy.tile;

import lisbam.pastoraleconomy.merchant.StationRole;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import java.util.UUID;

/** Local identity mirror for a StationRecord. The global record remains authoritative. */
public final class TileVillageStation extends TileEntity {
    private static final String KEY_STATION_ID = "stationId";
    private static final String KEY_VILLAGE_ID = "villageId";
    private static final String KEY_ROLE = "role";

    private UUID stationId;
    private UUID villageId;
    private StationRole role;

    public void bind(UUID newStationId, UUID newVillageId, StationRole newRole) {
        if (newStationId == null || newVillageId == null || newRole == null) {
            throw new IllegalArgumentException("Village station identity is incomplete.");
        }
        stationId = newStationId;
        villageId = newVillageId;
        role = newRole;
        markDirty();
    }

    public boolean isVillageStation() {
        return role == StationRole.VILLAGE && stationId != null && villageId != null;
    }

    public UUID getStationId() { return stationId; }
    public UUID getVillageId() { return villageId; }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (stationId != null) { compound.setUniqueId(KEY_STATION_ID, stationId); }
        if (villageId != null) { compound.setUniqueId(KEY_VILLAGE_ID, villageId); }
        if (role != null) { compound.setString(KEY_ROLE, role.name()); }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        stationId = compound.hasUniqueId(KEY_STATION_ID) ? compound.getUniqueId(KEY_STATION_ID) : null;
        villageId = compound.hasUniqueId(KEY_VILLAGE_ID) ? compound.getUniqueId(KEY_VILLAGE_ID) : null;
        role = null;
        String storedRole = compound.getString(KEY_ROLE);
        for (StationRole candidate : StationRole.values()) {
            if (candidate.name().equals(storedRole)) {
                role = candidate;
                break;
            }
        }
    }
}
