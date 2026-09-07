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

    /** A ticket may remain only while its original block instance still has redstone power. */
    public static boolean shouldKeepForcedChunk(boolean validChunkLoader, boolean receivingRedstonePower) {
        return validChunkLoader && receivingRedstonePower;
    }
}
