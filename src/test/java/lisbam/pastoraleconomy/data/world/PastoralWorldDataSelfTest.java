package lisbam.pastoraleconomy.data.world;

import net.minecraft.nbt.NBTTagCompound;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
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
        require(migratedNbt.hasKey("shippingBoxPayouts", 10),
                "legacy world data must gain an empty v10 shipping-payout segment");

        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000020");
        Map<UUID, Long> payout = new LinkedHashMap<UUID, Long>();
        payout.put(owner, Long.valueOf(250L));
        require(migrated.getShippingBoxPayoutState().tryBeginDispatch(9L),
                "world shipping state records its first processed day");
        require(migrated.getShippingBoxPayoutState().credit(payout),
                "world shipping state accepts deferred offline earnings");
        PastoralWorldData roundTrip = new PastoralWorldData();
        roundTrip.readFromNBT(migrated.writeToNBT(new NBTTagCompound()));
        require(roundTrip.getShippingBoxPayoutState().getLastDispatchDay() == 9L
                        && roundTrip.getShippingBoxPayoutState().getPending(owner) == 250L,
                "v10 shipping dispatch and pending payment survive world-data NBT round-trip");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
