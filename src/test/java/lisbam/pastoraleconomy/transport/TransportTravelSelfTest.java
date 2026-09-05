package lisbam.pastoraleconomy.transport;

import lisbam.pastoraleconomy.merchant.MerchantWorldState;
import lisbam.pastoraleconomy.merchant.StationRecord;
import lisbam.pastoraleconomy.merchant.StationRole;
import lisbam.pastoraleconomy.merchant.VillageRecord;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** Deterministic batch-15 checks that do not require a client or a loaded world. */
public final class TransportTravelSelfTest {
    private TransportTravelSelfTest() {
    }

    public static void main(String[] args) {
        long[] distances = {0L, 250L, 500L, 1000L, 2000L, 5000L, 10000L};
        long[] travel = {160L, 280L, 400L, 640L, 1120L, 2560L, 4960L};
        long[] connect = {400L, 700L, 1000L, 1600L, 2800L, 6400L, 12400L};
        for (int i = 0; i < distances.length; i++) {
            require(TransportCost.travelFee(distances[i]) == travel[i], "travel fee " + distances[i]);
            require(TransportCost.connectionFee(distances[i]) == connect[i], "connect fee " + distances[i]);
        }
        require(TransportCost.horizontalDistance(new BlockPos(0, 10, 0), new BlockPos(500, 200, 0)) == 500L,
                "Y must be ignored");

        UUID villageId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        MerchantWorldState state = new MerchantWorldState();
        VillageRecord village = new VillageRecord(villageId, 10, 70, 20);
        village.updateObservation(10, 70, 20, 2, true);
        village.setStationId(stationId);
        state.putVillage(village);
        state.putStation(new StationRecord(stationId, villageId, 0, new BlockPos(10, 70, 20), StationRole.VILLAGE));
        NBTTagCompound saved = state.writeToNBT();
        MerchantWorldState restored = new MerchantWorldState();
        restored.readFromNBT(saved);
        require(restored.getVillage(villageId) != null, "village identity persistence");
        require(restored.getStation(stationId) != null, "shared village station persistence");

        VillageTransportCandidate candidate = new VillageTransportCandidate(villageId, stationId,
                new BlockPos(10, 70, 20), 500L, 1000L, "村庄 #001");
        require(candidate.getVillageId().equals(villageId) && candidate.getStationId().equals(stationId),
                "candidate identity");
        System.out.println("TransportTravelSelfTest: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
