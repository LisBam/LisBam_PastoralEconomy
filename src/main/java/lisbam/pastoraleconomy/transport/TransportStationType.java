package lisbam.pastoraleconomy.transport;

/**
 * Stable physical node category. SELF_BUILT is the formal name used by batch
 * 15; PLAYER is retained only as an NBT compatibility spelling for early
 * batch-14 saves and is never emitted for new records.
 */
public enum TransportStationType {
    SELF_BUILT,
    VILLAGE;

    public static TransportStationType fromStoredName(String name) {
        if ("PLAYER".equals(name) || "SELF_BUILT".equals(name)) {
            return SELF_BUILT;
        }
        if ("VILLAGE".equals(name)) {
            return VILLAGE;
        }
        throw new IllegalArgumentException("Unknown transport station type: " + name);
    }
}
