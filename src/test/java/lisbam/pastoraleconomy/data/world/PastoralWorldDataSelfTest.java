package lisbam.pastoraleconomy.data.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/** Standalone data-boundary check for farm harassment across world-data migrations. */
public final class PastoralWorldDataSelfTest {
    private PastoralWorldDataSelfTest() {
    }

    public static void main(String[] args) {
        BlockPos origin = new BlockPos(0, 64, 0);
        PastoralWorldData data = new PastoralWorldData();
        for (int index = 0; index < PastoralWorldData.FARM_HARASSMENT_MAXIMUM; index++) {
            require(data.canRecordFarmHarassment(0, origin, 1000L + index), "quota must allow its first six records");
            data.recordFarmHarassment(0, origin, 1000L + index);
        }
        require(!data.canRecordFarmHarassment(0, origin, 1005L), "seventh local record must fail");
        require(data.canRecordFarmHarassment(0, new BlockPos(33, 64, 0), 1005L), "outside 32 blocks must be independent");
        require(data.canRecordFarmHarassment(1, origin, 1005L), "another dimension must be independent");

        NBTTagCompound saved = data.writeToNBT(new NBTTagCompound());
        PastoralWorldData restored = new PastoralWorldData();
        restored.readFromNBT(saved);
        require(!restored.canRecordFarmHarassment(0, origin, 1005L), "saved active quota must survive reload");
        require(restored.canRecordFarmHarassment(0, origin, 7006L), "records older than 6000 ticks must expire");

        NBTTagCompound v1 = new NBTTagCompound();
        v1.setInteger("dataVersion", 1);
        v1.setBoolean("firstInitializationCompleted", true);
        PastoralWorldData migrated = new PastoralWorldData();
        migrated.readFromNBT(v1);
        NBTTagCompound migratedNbt = migrated.writeToNBT(new NBTTagCompound());
        require(migratedNbt.getInteger("dataVersion") == PastoralWorldData.DATA_VERSION,
                "v1 data must write at the current schema version");
        require(migratedNbt.getBoolean("firstInitializationCompleted"), "v1 initialization marker must survive");
        require(migratedNbt.getTagList("farmHarassment", 10).tagCount() == 0,
                "v1 migration must start with an empty farm history");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
