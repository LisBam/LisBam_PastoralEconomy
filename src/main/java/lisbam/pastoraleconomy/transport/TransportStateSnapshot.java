package lisbam.pastoraleconomy.transport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Bounded, display-only S2C state for one opened transport-station GUI. */
public final class TransportStateSnapshot {
    private final UUID currentStationId;
    private final String currentAlias;
    private final boolean currentStationExists;
    private final boolean currentDimensionSupported;
    private final boolean currentActive;
    private final boolean starterFree;
    private final int dimension;
    private final int x;
    private final int y;
    private final int z;
    private final long coins;
    private final long connectionFee;
    private final List<TransportNodeView> nodes;
    private final VillageTransportCandidate nearestVillage;

    public TransportStateSnapshot(UUID currentStationId, String currentAlias, boolean currentStationExists,
                                  boolean currentDimensionSupported, boolean currentActive, boolean starterFree,
                                  int dimension, int x, int y, int z, long coins, long connectionFee,
                                  List<TransportNodeView> nodes) {
        this(currentStationId, currentAlias, currentStationExists, currentDimensionSupported, currentActive,
                starterFree, dimension, x, y, z, coins, connectionFee, nodes, null);
    }

    public TransportStateSnapshot(UUID currentStationId, String currentAlias, boolean currentStationExists,
                                  boolean currentDimensionSupported, boolean currentActive, boolean starterFree,
                                  int dimension, int x, int y, int z, long coins, long connectionFee,
                                  List<TransportNodeView> nodes, VillageTransportCandidate nearestVillage) {
        this.currentStationId = currentStationId;
        this.currentAlias = currentAlias == null ? "" : currentAlias;
        this.currentStationExists = currentStationExists;
        this.currentDimensionSupported = currentDimensionSupported;
        this.currentActive = currentActive;
        this.starterFree = starterFree;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.coins = Math.max(0L, coins);
        this.connectionFee = Math.max(0L, connectionFee);
        this.nodes = Collections.unmodifiableList(new ArrayList<TransportNodeView>(nodes));
        this.nearestVillage = nearestVillage;
    }

    public UUID getCurrentStationId() { return currentStationId; }
    public String getCurrentAlias() { return currentAlias; }
    public boolean hasCurrentStation() { return currentStationId != null; }
    public boolean currentStationExists() { return currentStationExists; }
    public boolean isCurrentDimensionSupported() { return currentDimensionSupported; }
    public boolean isCurrentActive() { return currentActive; }
    public boolean isStarterFree() { return starterFree; }
    public int getDimension() { return dimension; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public long getCoins() { return coins; }
    public long getConnectionFee() { return connectionFee; }
    public List<TransportNodeView> getNodes() { return nodes; }
    public VillageTransportCandidate getNearestVillage() { return nearestVillage; }
}
