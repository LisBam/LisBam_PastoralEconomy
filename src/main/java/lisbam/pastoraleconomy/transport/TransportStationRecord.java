package lisbam.pastoraleconomy.transport;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** Lightweight, world-owned identity and location of one physical station. */
public final class TransportStationRecord {
    private static final String KEY_ID = "stationId";
    private static final String KEY_DIMENSION = "dimension";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_Z = "z";
    private static final String KEY_TYPE = "type";
    private static final String KEY_VILLAGE = "villageId";

    private final UUID stationId;
    private final int dimension;
    private final BlockPos position;
    private final TransportStationType type;
    private final UUID villageId;

    public TransportStationRecord(UUID stationId, int dimension, BlockPos position, TransportStationType type) {
        this(stationId, dimension, position, type, null);
    }

    public TransportStationRecord(UUID stationId, int dimension, BlockPos position, TransportStationType type,
                                  UUID villageId) {
        if (stationId == null || position == null || type == null) {
            throw new IllegalArgumentException("Transport station identity is incomplete.");
        }
        this.stationId = stationId;
        this.dimension = dimension;
        this.position = position.toImmutable();
        this.type = type;
        this.villageId = villageId;
    }

    public UUID getStationId() {
        return stationId;
    }

    public int getDimension() {
        return dimension;
    }

    public BlockPos getPosition() {
        return position;
    }

    public TransportStationType getType() {
        return type;
    }

    public UUID getVillageId() {
        return villageId;
    }

    public boolean isAt(int expectedDimension, BlockPos expectedPosition) {
        return expectedPosition != null && dimension == expectedDimension && position.equals(expectedPosition);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId(KEY_ID, stationId);
        tag.setInteger(KEY_DIMENSION, dimension);
        tag.setInteger(KEY_X, position.getX());
        tag.setInteger(KEY_Y, position.getY());
        tag.setInteger(KEY_Z, position.getZ());
        tag.setString(KEY_TYPE, type.name());
        if (villageId != null) {
            tag.setUniqueId(KEY_VILLAGE, villageId);
        }
        return tag;
    }

    public static TransportStationRecord readFromNBT(NBTTagCompound tag) {
        if (!tag.hasUniqueId(KEY_ID)) {
            return null;
        }
        try {
            return new TransportStationRecord(tag.getUniqueId(KEY_ID), tag.getInteger(KEY_DIMENSION),
                    new BlockPos(tag.getInteger(KEY_X), tag.getInteger(KEY_Y), tag.getInteger(KEY_Z)),
                    TransportStationType.fromStoredName(tag.getString(KEY_TYPE)),
                    tag.hasUniqueId(KEY_VILLAGE) ? tag.getUniqueId(KEY_VILLAGE) : null);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
