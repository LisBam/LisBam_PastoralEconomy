package lisbam.pastoraleconomy.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lisbam.pastoraleconomy.client.ClientMarketState;
import lisbam.pastoraleconomy.market.MarketHistoryPoint;
import lisbam.pastoraleconomy.market.MarketHistorySnapshot;
import lisbam.pastoraleconomy.network.message.RequestMarketHistoryMessage;
import lisbam.pastoraleconomy.network.message.SyncMarketHistoryMessage;

import java.util.ArrayList;
import java.util.List;

/** Standalone regression checks for bounded packet decoding and stale GUI snapshots. */
public final class MarketPacketSelfTest {
    private static final String WHEAT = "lisbam_pastoral_economy:sell/crop/wheat";

    private MarketPacketSelfTest() {
    }

    public static void main(String[] args) {
        verifyRequestRoundTripAndBounds();
        verifySnapshotRoundTripAndBounds();
        verifyStaleSnapshotDoesNotReplaceNewerWindow();
        verifyBookSessionDropsLateSnapshots();
    }

    private static void verifyRequestRoundTripAndBounds() {
        RequestMarketHistoryMessage outbound = new RequestMarketHistoryMessage(WHEAT, 42L, 7);
        ByteBuf buffer = Unpooled.buffer();
        outbound.toBytes(buffer);
        RequestMarketHistoryMessage decoded = new RequestMarketHistoryMessage();
        decoded.fromBytes(buffer);
        require(decoded.isValid(), "valid request must round-trip");
        require(WHEAT.equals(decoded.getCommodityKey()) && decoded.getBeforeExclusiveDay() == 42L
                        && decoded.getRequestId() == 7,
                "request payload must preserve only key, cursor and request id");

        ByteBuf oversizedKey = Unpooled.buffer();
        oversizedKey.writeShort(129);
        oversizedKey.writeZero(129);
        oversizedKey.writeLong(0L);
        oversizedKey.writeInt(1);
        RequestMarketHistoryMessage rejected = new RequestMarketHistoryMessage();
        rejected.fromBytes(oversizedKey);
        require(!rejected.isValid(), "oversized commodity keys must be rejected before allocation");
    }

    private static void verifySnapshotRoundTripAndBounds() {
        List<MarketHistoryPoint> points = new ArrayList<MarketHistoryPoint>();
        points.add(new MarketHistoryPoint(40L, 5L, null));
        points.add(new MarketHistoryPoint(41L, 6L, Long.valueOf(5L)));
        MarketHistorySnapshot snapshot = new MarketHistorySnapshot(
                8, WHEAT, -1L, 41L, 6L, Long.valueOf(5L), points, false, false
        );
        ByteBuf buffer = Unpooled.buffer();
        new SyncMarketHistoryMessage(snapshot).toBytes(buffer);
        SyncMarketHistoryMessage decodedMessage = new SyncMarketHistoryMessage();
        decodedMessage.fromBytes(buffer);
        MarketHistorySnapshot decoded = decodedMessage.getSnapshot();
        require(decoded != null && decoded.getPoints().size() == 2, "bounded snapshot must round-trip");
        require(decoded.getPoints().get(0).getPreviousPrice() == null
                        && decoded.getPoints().get(1).getPreviousPrice().longValue() == 5L,
                "the first visible point must retain its real prior-point state");

        ByteBuf oversizedPoints = Unpooled.buffer();
        writeKey(oversizedPoints, WHEAT);
        oversizedPoints.writeLong(-1L);
        oversizedPoints.writeInt(1);
        oversizedPoints.writeLong(1L);
        oversizedPoints.writeLong(1L);
        oversizedPoints.writeBoolean(false);
        oversizedPoints.writeBoolean(false);
        oversizedPoints.writeBoolean(false);
        oversizedPoints.writeInt(31);
        SyncMarketHistoryMessage rejected = new SyncMarketHistoryMessage();
        rejected.fromBytes(oversizedPoints);
        require(rejected.getSnapshot() == null, "more than thirty received history points must be rejected");
    }

    private static void verifyStaleSnapshotDoesNotReplaceNewerWindow() {
        List<MarketHistoryPoint> points = new ArrayList<MarketHistoryPoint>();
        points.add(new MarketHistoryPoint(1L, 5L, null));
        MarketHistorySnapshot stale = new MarketHistorySnapshot(
                1, WHEAT, -1L, 1L, 5L, null, points, false, false
        );
        MarketHistorySnapshot fresh = new MarketHistorySnapshot(
                2, WHEAT, -1L, 1L, 5L, null, points, false, false
        );
        ClientMarketState.clear();
        ClientMarketState.acceptSnapshot(fresh);
        ClientMarketState.acceptSnapshot(stale);
        require(ClientMarketState.getSnapshot(WHEAT, -1L, 2) == fresh,
                "a late response cannot replace a newer snapshot for the same window");
        ClientMarketState.clear();
    }

    private static void verifyBookSessionDropsLateSnapshots() {
        List<MarketHistoryPoint> points = new ArrayList<MarketHistoryPoint>();
        points.add(new MarketHistoryPoint(1L, 5L, null));
        ClientMarketState.beginBookSession();
        int firstRequest = ClientMarketState.nextRequestId();
        MarketHistorySnapshot first = new MarketHistorySnapshot(
                firstRequest, WHEAT, -1L, 1L, 5L, null, points, false, false
        );
        ClientMarketState.acceptSnapshot(first);
        require(ClientMarketState.getSnapshot(WHEAT, -1L, firstRequest) == first,
                "an open book session must accept its own snapshot");

        ClientMarketState.endBookSession();
        ClientMarketState.acceptSnapshot(first);
        require(ClientMarketState.getSnapshot(WHEAT, -1L, firstRequest) == null,
                "a closed book must discard late market packets");

        ClientMarketState.beginBookSession();
        int secondRequest = ClientMarketState.nextRequestId();
        require(secondRequest > firstRequest, "reopened books must not reuse request ids in one connection");
        ClientMarketState.endBookSession();
    }

    private static void writeKey(ByteBuf buffer, String key) {
        byte[] bytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
