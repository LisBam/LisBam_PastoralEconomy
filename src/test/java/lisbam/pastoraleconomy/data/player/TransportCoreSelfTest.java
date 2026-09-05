package lisbam.pastoraleconomy.data.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lisbam.pastoraleconomy.network.message.SyncTransportStateMessage;
import lisbam.pastoraleconomy.transport.TransportCost;
import lisbam.pastoraleconomy.transport.TransportNameRules;
import lisbam.pastoraleconomy.transport.TransportNodeView;
import lisbam.pastoraleconomy.transport.TransportStationRecord;
import lisbam.pastoraleconomy.transport.TransportStationType;
import lisbam.pastoraleconomy.transport.TransportStateSnapshot;
import lisbam.pastoraleconomy.transport.TransportWorldState;
import lisbam.pastoraleconomy.transport.VillageTransportCandidate;
import lisbam.pastoraleconomy.transport.VillageTransportCompassTarget;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/** Standalone deterministic coverage for the batch-14 node-layer invariants. */
public final class TransportCoreSelfTest {
    private TransportCoreSelfTest() {
    }

    public static void main(String[] args) {
        verifyCostFormula();
        verifyNames();
        verifyPlayerPersistence();
        verifyWorldRegistry();
        verifyVillageCompassTarget();
        verifyPacketRoundTrip();
    }

    private static void verifyCostFormula() {
        long[] distances = new long[] { 0L, 250L, 500L, 1000L, 2000L, 5000L, 10000L };
        long[] fees = new long[] { 400L, 700L, 1000L, 1600L, 2800L, 6400L, 12400L };
        for (int index = 0; index < distances.length; index++) {
            require(TransportCost.connectionFee(distances[index]) == fees[index], "frozen fee point " + distances[index]);
        }
        long[] travelFees = new long[] { 160L, 280L, 400L, 640L, 1120L, 2560L, 4960L };
        for (int index = 0; index < distances.length; index++) {
            require(TransportCost.travelFee(distances[index]) == travelFees[index], "travel fee point " + distances[index]);
        }
        require(TransportCost.horizontalDistance(new BlockPos(1, -64, 2), new BlockPos(1, 320, 2)) == 0L,
                "Y must not affect transport distance");
        require(TransportCost.horizontalDistance(new BlockPos(-30000000, 0, -30000000),
                new BlockPos(30000000, 255, 30000000)) > 0L, "large horizontal coordinates must not overflow");
    }

    private static void verifyNames() {
        require(TransportNameRules.isValidAlias("家"), "Chinese alias must be valid");
        require(TransportNameRules.isValidAlias("Farm 一号"), "mixed alias must be valid");
        require(!TransportNameRules.isValidAlias("   "), "whitespace-only alias must fail");
        require(!TransportNameRules.isValidAlias("a\n"), "newline alias must fail");
        require(!TransportNameRules.isValidAlias("§a家"), "format-code alias must fail");
        StringBuilder twentyFour = new StringBuilder();
        StringBuilder twentyFive = new StringBuilder();
        for (int index = 0; index < 24; index++) {
            twentyFour.appendCodePoint(0x1F33F);
            twentyFive.appendCodePoint(0x1F33F);
        }
        twentyFive.appendCodePoint(0x1F33F);
        require(TransportNameRules.isValidAlias(twentyFour.toString()), "24 supplementary code points must pass");
        require(!TransportNameRules.isValidAlias(twentyFive.toString()), "25 supplementary code points must fail");
        require("家 #2".equals(TransportNameRules.nextHomeAlias(Arrays.asList("家"))), "home suffix rule");
        require("自建站点 #001".equals(TransportNameRules.selfBuiltAlias(1, new ArrayList<String>())),
                "self-built default rule");
    }

    private static void verifyPlayerPersistence() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PlayerData data = new PlayerData();
        data.markStarterTransportGranted();
        require(data.hasStarterTransportBeenGranted(), "starter gift must persist as granted");
        require(data.activateTransportNode(first, "家"), "first node activation");
        data.markFirstSelfBuiltStationEstablished(true);
        require(data.hasEstablishedFirstSelfBuiltStation() && data.hasUsedFirstSelfBuiltStationFree(),
                "free first-node history");
        require(data.removeTransportNode(first), "remove deletes player node");
        require(data.getTransportNode(first) == null, "removed node must not stay recorded");
        require(data.activateTransportNode(second, "自建站点 #001"), "second node activation");
        NBTTagCompound saved = data.writeToNBT();
        PlayerData restored = new PlayerData();
        restored.readFromNBT(saved);
        require(restored.hasStarterTransportBeenGranted(), "starter state NBT round trip");
        require(restored.hasEstablishedFirstSelfBuiltStation() && restored.hasUsedFirstSelfBuiltStationFree(),
                "free history NBT round trip");
        require(restored.getTransportNode(first) == null && restored.getTransportNode(second).isActive(),
                "only active nodes survive the NBT round trip");
        PlayerData clone = new PlayerData();
        clone.copyFrom(restored);
        require(clone.getTransportNodes().size() == 1 && clone.getTransportNode(second).isActive(),
                "death clone must preserve active transport nodes");
    }

    private static void verifyWorldRegistry() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        TransportWorldState state = new TransportWorldState();
        require(state.putStation(new TransportStationRecord(first, 0, new BlockPos(0, 64, 0),
                TransportStationType.SELF_BUILT)), "register first physical station");
        require(state.getStationAt(0, new BlockPos(0, 64, 0)).getStationId().equals(first), "lookup by position");
        require(state.removeStation(first, 0, new BlockPos(0, 64, 0)), "physical break unregisters station");
        require(state.getStation(first) == null, "broken station no longer exists");
        state.putStation(new TransportStationRecord(second, 0, new BlockPos(250, 80, 0), TransportStationType.SELF_BUILT));
        TransportWorldState restored = new TransportWorldState();
        restored.readFromNBT(state.writeToNBT());
        require(restored.getStation(second) != null && restored.getStation(second).getPosition().getX() == 250,
                "world registry NBT round trip");
    }

    private static void verifyPacketRoundTrip() {
        UUID station = UUID.randomUUID();
        TransportNodeView node = new TransportNodeView(station, true, "家", true, 0, 1, 64, 2,
                TransportStationType.SELF_BUILT, 105L);
        TransportStateSnapshot snapshot = new TransportStateSnapshot(station, "家", true, true, true, false,
                0, 1, 64, 2, 12580L, 0L, Arrays.asList(node),
                new VillageTransportCandidate(UUID.randomUUID(), null, new BlockPos(500, 70, 0), 500L, 1000L, "村庄 #001"));
        ByteBuf buffer = Unpooled.buffer();
        new SyncTransportStateMessage(snapshot).toBytes(buffer);
        SyncTransportStateMessage decoded = new SyncTransportStateMessage();
        decoded.fromBytes(buffer);
        require(decoded.getSnapshot() != null && decoded.getSnapshot().getNodes().size() == 1
                && "家".equals(decoded.getSnapshot().getNodes().get(0).getAlias())
                && decoded.getSnapshot().getNodes().get(0).getTravelFee() == 105L
                && decoded.getSnapshot().getNearestVillage() != null
                && decoded.getSnapshot().getNearestVillage().getStationId() == null,
                "bounded transport sync round trip");
    }

    private static void verifyVillageCompassTarget() {
        TransportWorldState state = new TransportWorldState();
        UUID selfBuilt = UUID.randomUUID();
        UUID wrongDimension = UUID.randomUUID();
        UUID fartherVillage = UUID.randomUUID();
        UUID nearestVillage = UUID.randomUUID();
        state.putStation(new TransportStationRecord(selfBuilt, 0, new BlockPos(1, 64, 1),
                TransportStationType.SELF_BUILT));
        state.putStation(new TransportStationRecord(wrongDimension, -1, new BlockPos(0, 64, 0),
                TransportStationType.VILLAGE));
        state.putStation(new TransportStationRecord(fartherVillage, 0, new BlockPos(160, 5, 0),
                TransportStationType.VILLAGE));
        state.putStation(new TransportStationRecord(nearestVillage, 0, new BlockPos(10, 200, 0),
                TransportStationType.VILLAGE));
        TransportStationRecord target = VillageTransportCompassTarget.findNearest(state.getStations(), 0,
                new BlockPos(0, 64, 0));
        require(target != null && nearestVillage.equals(target.getStationId()),
                "compass selects only the nearest same-dimension village station by horizontal distance");
        require(VillageTransportCompassTarget.findNearest(state.getStations(), 1, new BlockPos(0, 64, 0)) == null,
                "compass has no cross-dimension village target");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
