package lisbam.pastoraleconomy.chunkloader;

/** Pure constants and decisions for the redstone-controlled chunk loader. */
public final class ChunkLoaderRules {
    /** Vanilla torch brightness is 14, so the active loader emits exactly half. */
    public static final int ACTIVE_LIGHT_LEVEL = 7;

    private ChunkLoaderRules() {
    }

    public static boolean shouldBeActive(boolean receivingRedstonePower) {
        return receivingRedstonePower;
    }
}
