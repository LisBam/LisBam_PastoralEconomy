package lisbam.pastoraleconomy.tile;

import lisbam.pastoraleconomy.transport.TransportService;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import java.util.UUID;

/** Local physical identity only; player activation and aliases never live here. */
public final class TileTransportStation extends TileEntity {
    private static final String KEY_STATION_ID = "stationId";
    private static final String KEY_STARTER_OWNER = "starterOwner";

    private UUID stationId;
    private UUID starterOwner;

    public UUID getStationId() {
        return stationId;
    }

    public UUID getStarterOwner() {
        return starterOwner;
    }

    public boolean isStarterFor(UUID playerId) {
        return playerId != null && playerId.equals(starterOwner);
    }

    public void assignStationId(UUID newStationId) {
        if (newStationId == null) {
            throw new IllegalArgumentException("Transport station UUID is required.");
        }
        stationId = newStationId;
        markDirty();
    }

    public void setStarterOwner(UUID owner) {
        starterOwner = owner;
        markDirty();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isRemote) {
            TransportService.onStationTileLoaded(world, pos, this);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (stationId != null) {
            compound.setUniqueId(KEY_STATION_ID, stationId);
        }
        if (starterOwner != null) {
            compound.setUniqueId(KEY_STARTER_OWNER, starterOwner);
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        stationId = compound.hasUniqueId(KEY_STATION_ID) ? compound.getUniqueId(KEY_STATION_ID) : null;
        starterOwner = compound.hasUniqueId(KEY_STARTER_OWNER) ? compound.getUniqueId(KEY_STARTER_OWNER) : null;
    }
}
