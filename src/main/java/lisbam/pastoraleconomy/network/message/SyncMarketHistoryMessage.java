package lisbam.pastoraleconomy.network.message;

import io.netty.buffer.ByteBuf;
import lisbam.pastoraleconomy.market.MarketHistoryPoint;
import lisbam.pastoraleconomy.market.MarketHistorySnapshot;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Bounded S2C market display snapshot. It deliberately contains no WorldSavedData NBT. */
public final class SyncMarketHistoryMessage implements IMessage {
    private static final int MAX_POINTS = 30;

    @Nullable
    private MarketHistorySnapshot snapshot;

    public SyncMarketHistoryMessage() {
    }

    public SyncMarketHistoryMessage(MarketHistorySnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Market history snapshot is required.");
        }
        this.snapshot = snapshot;
    }

    @Nullable
    public MarketHistorySnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        snapshot = null;
        String commodityKey = MarketPacketCodec.readCommodityKey(buffer);
        if (commodityKey == null || buffer.readableBytes() < 8 + 4 + 8 + 8 + 1) {
            return;
        }

        long beforeExclusiveDay = buffer.readLong();
        int requestId = buffer.readInt();
        long currentMarketDay = buffer.readLong();
        long currentPrice = buffer.readLong();
        boolean hasPreviousPrice = buffer.readBoolean();
        Long previousPrice = null;
        if (hasPreviousPrice) {
            if (buffer.readableBytes() < 8) {
                return;
            }
            previousPrice = Long.valueOf(buffer.readLong());
        }
        if (buffer.readableBytes() < 2 + 4) {
            return;
        }
        boolean hasOlderHistory = buffer.readBoolean();
        boolean hasNewerHistory = buffer.readBoolean();
        int pointCount = buffer.readInt();
        if (pointCount < 0 || pointCount > MAX_POINTS) {
            return;
        }

        List<MarketHistoryPoint> points = new ArrayList<MarketHistoryPoint>(pointCount);
        long previousDay = -1L;
        for (int index = 0; index < pointCount; index++) {
            if (buffer.readableBytes() < 8 + 8 + 1) {
                return;
            }
            long worldDay = buffer.readLong();
            long price = buffer.readLong();
            boolean hasPreviousPoint = buffer.readBoolean();
            Long previousPointPrice = null;
            if (hasPreviousPoint) {
                if (buffer.readableBytes() < 8) {
                    return;
                }
                previousPointPrice = Long.valueOf(buffer.readLong());
            }
            if (worldDay < 0L || worldDay > currentMarketDay || worldDay <= previousDay || price <= 0L
                    || (previousPointPrice != null && previousPointPrice.longValue() <= 0L)) {
                return;
            }
            previousDay = worldDay;
            points.add(new MarketHistoryPoint(worldDay, price, previousPointPrice));
        }

        if (beforeExclusiveDay < -1L || (beforeExclusiveDay >= 0L && beforeExclusiveDay > currentMarketDay)
                || currentMarketDay < 0L || currentPrice <= 0L
                || (previousPrice != null && previousPrice.longValue() <= 0L)) {
            return;
        }
        snapshot = new MarketHistorySnapshot(
                requestId,
                commodityKey,
                beforeExclusiveDay,
                currentMarketDay,
                currentPrice,
                previousPrice,
                points,
                hasOlderHistory,
                hasNewerHistory
        );
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (snapshot == null) {
            throw new IllegalStateException("Cannot encode an absent market history snapshot.");
        }
        MarketPacketCodec.writeCommodityKey(buffer, snapshot.getCommodityKey());
        buffer.writeLong(snapshot.getBeforeExclusiveDay());
        buffer.writeInt(snapshot.getRequestId());
        buffer.writeLong(snapshot.getCurrentMarketDay());
        buffer.writeLong(snapshot.getCurrentPrice());
        Long previousPrice = snapshot.getPreviousPrice();
        buffer.writeBoolean(previousPrice != null);
        if (previousPrice != null) {
            buffer.writeLong(previousPrice.longValue());
        }
        buffer.writeBoolean(snapshot.hasOlderHistory());
        buffer.writeBoolean(snapshot.hasNewerHistory());
        List<MarketHistoryPoint> points = snapshot.getPoints();
        buffer.writeInt(points.size());
        for (MarketHistoryPoint point : points) {
            buffer.writeLong(point.getWorldDay());
            buffer.writeLong(point.getPrice());
            buffer.writeBoolean(point.hasPreviousPoint());
            if (point.hasPreviousPoint()) {
                buffer.writeLong(point.getPreviousPrice().longValue());
            }
        }
    }
}
