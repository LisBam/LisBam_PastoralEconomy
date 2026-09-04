package lisbam.pastoraleconomy.merchant;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** World-persisted station identity. It never stores live world or tile references. */
public final class StationRecord {
    private static final String KEY_ID = "stationId";
    private static final String KEY_VILLAGE = "villageId";
    private static final String KEY_DIMENSION = "dimension";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_Z = "z";
    private static final String KEY_ROLE = "role";

    private final UUID stationId;
    private final UUID villageId;
    private int dimension;
    private BlockPos position;
    private final StationRole role;

    public StationRecord(UUID stationId, UUID villageId, int dimension, BlockPos position, StationRole role) {
        if (stationId == null || villageId == null || position == null || role == null) {
            throw new IllegalArgumentException("Station identity is incomplete.");
        }
        this.stationId = stationId;
        this.villageId = villageId;
        this.dimension = dimension;
        this.position = position.toImmutable();
        this.role = role;
    }

    public UUID getStationId() { return stationId; }
    public UUID getVillageId() { return villageId; }
    public int getDimension() { return dimension; }
    public BlockPos getPosition() { return position; }
    public StationRole getRole() { return role; }

    public void moveTo(int newDimension, BlockPos newPosition) {
        if (newPosition == null) {
            throw new IllegalArgumentException("Station position is required.");
        }
        dimension = newDimension;
        position = newPosition.toImmutable();
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId(KEY_ID, stationId);
        tag.setUniqueId(KEY_VILLAGE, villageId);
        tag.setInteger(KEY_DIMENSION, dimension);
        tag.setInteger(KEY_X, position.getX());
        tag.setInteger(KEY_Y, position.getY());
        tag.setInteger(KEY_Z, position.getZ());
        tag.setString(KEY_ROLE, role.name());
        return tag;
    }

    public static StationRecord readFromNBT(NBTTagCompound tag) {
        if (!tag.hasUniqueId(KEY_ID) || !tag.hasUniqueId(KEY_VILLAGE)) {
            return null;
        }
        try {
            StationRole role = StationRole.valueOf(tag.getString(KEY_ROLE));
            return new StationRecord(tag.getUniqueId(KEY_ID), tag.getUniqueId(KEY_VILLAGE),
                    tag.getInteger(KEY_DIMENSION), new BlockPos(tag.getInteger(KEY_X), tag.getInteger(KEY_Y),
                    tag.getInteger(KEY_Z)), role);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
