package lisbam.pastoraleconomy.data.world;

import net.minecraft.nbt.NBTTagCompound;
/** Standalone data-boundary check for retained world-data migrations. */
public final class PastoralWorldDataSelfTest {
    private PastoralWorldDataSelfTest() {
    }

    public static void main(String[] args) {
        NBTTagCompound v1 = new NBTTagCompound();
        v1.setInteger("dataVersion", 1);
        v1.setBoolean("firstInitializationCompleted", true);
        PastoralWorldData migrated = new PastoralWorldData();
        migrated.readFromNBT(v1);
        NBTTagCompound migratedNbt = migrated.writeToNBT(new NBTTagCompound());
        require(migratedNbt.getInteger("dataVersion") == PastoralWorldData.DATA_VERSION,
                "v1 data must write at the current schema version");
        require(migratedNbt.getBoolean("firstInitializationCompleted"), "v1 initialization marker must survive");
        require(!migratedNbt.hasKey("farmHarassment", 9),
                "obsolete farm-harassment state must not be written");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
