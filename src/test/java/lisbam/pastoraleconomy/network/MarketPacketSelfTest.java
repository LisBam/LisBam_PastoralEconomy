package lisbam.pastoraleconomy.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lisbam.pastoraleconomy.client.ClientMarketState;
import lisbam.pastoraleconomy.gui.ContainerMarketBook;
import lisbam.pastoraleconomy.market.MarketHistoryPoint;
import lisbam.pastoraleconomy.market.MarketHistorySnapshot;
import lisbam.pastoraleconomy.network.message.RequestMarketHistoryMessage;
import lisbam.pastoraleconomy.network.message.SyncMarketHistoryMessage;

import java.util.ArrayList;
import java.util.List;

/** Standalone regression checks for bounded packet decoding and stale GUI snapshots. */
public final class MarketPacketSelfTest {
    private static final String WHEAT = "lisbam_pastoral_economy:sell/crop/wheat";
    private static final String CARROT = "lisbam_pastoral_economy:sell/crop/carrot";

    private MarketPacketSelfTest() {
    }

    public static void main(String[] args) {
        verifyRequestRoundTripAndBounds();
        verifySnapshotRoundTripAndBounds();
        verifyStaleSnapshotDoesNotReplaceNewerWindow();
        verifyBookSessionDropsLateSnapshots();
        verifyProgressivePrefetchCachesSelectableCommodities();
        verifyBookRequestCoalescing();
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
        points.add(new MarketHistoryPoint(40L, 50L, null));
        points.add(new MarketHistoryPoint(41L, 60L, Long.valueOf(50L)));
        MarketHistorySnapshot snapshot = new MarketHistorySnapshot(
                8, WHEAT, -1L, 41L, 60L, Long.valueOf(50L), points, false, false
        );
        ByteBuf buffer = Unpooled.buffer();
        new SyncMarketHistoryMessage(snapshot).toBytes(buffer);
        SyncMarketHistoryMessage decodedMessage = new SyncMarketHistoryMessage();
        decodedMessage.fromBytes(buffer);
        MarketHistorySnapshot decoded = decodedMessage.getSnapshot();
        require(decoded != null && decoded.getPoints().size() == 2, "bounded snapshot must round-trip");
        require(decoded.getPoints().get(0).getPreviousPrice() == null
                        && decoded.getPoints().get(1).getPreviousPrice().longValue() == 50L,
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
        points.add(new MarketHistoryPoint(1L, 50L, null));
        MarketHistorySnapshot stale = new MarketHistorySnapshot(
                1, WHEAT, -1L, 1L, 50L, null, points, false, false
        );
        MarketHistorySnapshot fresh = new MarketHistorySnapshot(
                2, WHEAT, -1L, 1L, 50L, null, points, false, false
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
        points.add(new MarketHistoryPoint(1L, 50L, null));
        ClientMarketState.beginBookSession();
        int firstRequest = ClientMarketState.nextRequestId();
        MarketHistorySnapshot first = new MarketHistorySnapshot(
                firstRequest, WHEAT, -1L, 1L, 50L, null, points, false, false
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

    private static void verifyBookRequestCoalescing() {
        ContainerMarketBook book = new ContainerMarketBook();
        RequestMarketHistoryMessage first = new RequestMarketHistoryMessage(WHEAT, -1L, 1);
        RequestMarketHistoryMessage second = new RequestMarketHistoryMessage("lisbam_pastoral_economy:sell/crop/carrot", -1L, 2);
        RequestMarketHistoryMessage latest = new RequestMarketHistoryMessage("lisbam_pastoral_economy:sell/crop/potato", -1L, 3);

        book.queueMarketRequest(first);
        require(book.pollMarketRequest(100L) == first, "the first open-book request must be served immediately");
        book.queueMarketRequest(second);
        require(book.pollMarketRequest(100L) == null, "the cooldown must defer rapid requests instead of dropping them");
        book.queueMarketRequest(latest);
        require(book.pollMarketRequest(101L) == null, "the latest request must remain queued until the cooldown expires");
        require(book.pollMarketRequest(102L) == latest, "only the latest rapid crop selection must be served");
    }

    private static void verifyProgressivePrefetchCachesSelectableCommodities() {
        List<MarketHistoryPoint> points = new ArrayList<MarketHistoryPoint>();
        points.add(new MarketHistoryPoint(1L, 50L, null));
        ClientMarketState.beginBookSession();
        int wheatRequest = ClientMarketState.nextRequestId();
        int carrotRequest = ClientMarketState.nextRequestId();
        MarketHistorySnapshot wheat = new MarketHistorySnapshot(
                wheatRequest, WHEAT, -1L, 1L, 50L, null, points, false, false
        );
        MarketHistorySnapshot carrot = new MarketHistorySnapshot(
                carrotRequest, CARROT, -1L, 1L, 50L, null, points, false, false
        );
        ClientMarketState.acceptSnapshot(wheat);
        ClientMarketState.acceptSnapshot(carrot);
        require(ClientMarketState.getLatestSnapshot(CARROT) == carrot,
                "a low-priority prefetch window must become immediately selectable when it arrives");
        require(ClientMarketState.getSnapshot(WHEAT, -1L, wheatRequest) == wheat,
                "the selected commodity must retain its own request identity while other windows prefetch");
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
