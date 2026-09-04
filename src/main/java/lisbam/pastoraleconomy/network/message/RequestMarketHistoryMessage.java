package lisbam.pastoraleconomy.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Bounded C2S request containing only a stable commodity key and a page cursor. */
public final class RequestMarketHistoryMessage implements IMessage {
    private String commodityKey;
    private long beforeExclusiveDay;
    private int requestId;
    private boolean valid;

    public RequestMarketHistoryMessage() {
    }

    public RequestMarketHistoryMessage(String commodityKey, long beforeExclusiveDay, int requestId) {
        if (beforeExclusiveDay < -1L) {
            throw new IllegalArgumentException("Market history cursor must be -1 or a world day.");
        }
        this.commodityKey = commodityKey;
        this.beforeExclusiveDay = beforeExclusiveDay;
        this.requestId = requestId;
        this.valid = commodityKey != null && commodityKey.length() > 0;
    }

    public String getCommodityKey() {
        return commodityKey;
    }

    public long getBeforeExclusiveDay() {
        return beforeExclusiveDay;
    }

    public int getRequestId() {
        return requestId;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        valid = false;
        String decodedKey = MarketPacketCodec.readCommodityKey(buffer);
        if (decodedKey == null || buffer.readableBytes() < 12) {
            return;
        }
        long decodedCursor = buffer.readLong();
        int decodedRequestId = buffer.readInt();
        if (decodedCursor < -1L) {
            return;
        }
        commodityKey = decodedKey;
        beforeExclusiveDay = decodedCursor;
        requestId = decodedRequestId;
        valid = true;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        MarketPacketCodec.writeCommodityKey(buffer, commodityKey);
        buffer.writeLong(beforeExclusiveDay);
        buffer.writeInt(requestId);
    }
}
