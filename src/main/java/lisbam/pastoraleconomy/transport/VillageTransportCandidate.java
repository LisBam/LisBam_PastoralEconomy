package lisbam.pastoraleconomy.transport;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** Immutable server-computed preview of one nearest not-yet-active village. */
public final class VillageTransportCandidate {
    private final UUID villageId;
    private final UUID stationId;
    private final BlockPos position;
    private final long distance;
    private final long connectionFee;
    private final String displayName;

    public VillageTransportCandidate(UUID villageId, UUID stationId, BlockPos position,
                                     long distance, long connectionFee, String displayName) {
        if (villageId == null || position == null || distance < 0L || connectionFee < 0L) {
            throw new IllegalArgumentException("Village candidate is incomplete.");
        }
        this.villageId = villageId;
        this.stationId = stationId;
        this.position = position.toImmutable();
        this.distance = distance;
        this.connectionFee = connectionFee;
        this.displayName = displayName == null || displayName.isEmpty() ? "村庄" : displayName;
    }

    public UUID getVillageId() { return villageId; }
    public UUID getStationId() { return stationId; }
    public BlockPos getPosition() { return position; }
    public long getDistance() { return distance; }
    public long getConnectionFee() { return connectionFee; }
    public String getDisplayName() { return displayName; }
}
