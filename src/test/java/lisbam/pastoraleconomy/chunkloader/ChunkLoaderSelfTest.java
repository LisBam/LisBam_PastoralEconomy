package lisbam.pastoraleconomy.chunkloader;

/** Deterministic checks for the redstone/light boundary of the chunk loader. */
public final class ChunkLoaderSelfTest {
    private ChunkLoaderSelfTest() {
    }

    public static void main(String[] args) {
        require(ChunkLoaderRules.ACTIVE_LIGHT_LEVEL == 7,
                "active chunk loader light must equal half of a torch's level 14 light");
        require(ChunkLoaderRules.shouldBeActive(true), "a redstone signal must activate the chunk loader");
        require(!ChunkLoaderRules.shouldBeActive(false), "loss of redstone signal must deactivate the chunk loader");
        System.out.println("chunkLoaderSelfTest PASS");
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
