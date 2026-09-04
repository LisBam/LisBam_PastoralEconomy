package lisbam.pastoraleconomy.transport;

import java.util.UUID;

/** Immutable client-display projection of one player's known station record. */
public final class TransportNodeView {
    private final UUID stationId;
    private final boolean active;
    private final String alias;
    private final boolean exists;
    private final int dimension;
    private final int x;
    private final int y;
    private final int z;
    private final TransportStationType type;
    /** -1 means this node is not a currently usable destination. */
    private final long travelFee;

    public TransportNodeView(UUID stationId, boolean active, String alias, boolean exists, int dimension,
                             int x, int y, int z, TransportStationType type) {
        this(stationId, active, alias, exists, dimension, x, y, z, type, -1L);
    }

    public TransportNodeView(UUID stationId, boolean active, String alias, boolean exists, int dimension,
                             int x, int y, int z, TransportStationType type, long travelFee) {
        if (stationId == null || alias == null || type == null) {
            throw new IllegalArgumentException("Transport node view is incomplete.");
        }
        if (travelFee < -1L) {
            throw new IllegalArgumentException("Travel fee cannot be below the unavailable sentinel.");
        }
        this.stationId = stationId;
        this.active = active;
        this.alias = alias;
        this.exists = exists;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.travelFee = travelFee;
    }

    public UUID getStationId() { return stationId; }
    public boolean isActive() { return active; }
    public String getAlias() { return alias; }
    public boolean exists() { return exists; }
    public int getDimension() { return dimension; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public TransportStationType getType() { return type; }
    public boolean hasTravelFee() { return travelFee >= 0L; }
    public long getTravelFee() { return travelFee; }
}
