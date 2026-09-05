package lisbam.pastoraleconomy.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Small S2C reply carrying one server-frozen current purchase price. */
public final class SyncMarketTooltipPriceMessage implements IMessage {
    private String commodityKey;
    private long marketDay;
    private long price;
    private boolean valid;

    public SyncMarketTooltipPriceMessage() {
    }

    public SyncMarketTooltipPriceMessage(String commodityKey, long marketDay, long price) {
        if (commodityKey == null || commodityKey.isEmpty() || marketDay < 0L || price <= 0L) {
            throw new IllegalArgumentException("Invalid market tooltip price response.");
        }
        this.commodityKey = commodityKey;
        this.marketDay = marketDay;
        this.price = price;
        this.valid = true;
    }

    public String getCommodityKey() {
        return commodityKey;
    }

    public long getMarketDay() {
        return marketDay;
    }

    public long getPrice() {
        return price;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        valid = false;
        String decodedKey = MarketPacketCodec.readCommodityKey(buffer);
        if (decodedKey == null || buffer.readableBytes() < 16) {
            return;
        }
        long decodedMarketDay = buffer.readLong();
        long decodedPrice = buffer.readLong();
        if (decodedMarketDay < 0L || decodedPrice <= 0L) {
            return;
        }
        commodityKey = decodedKey;
        marketDay = decodedMarketDay;
        price = decodedPrice;
        valid = true;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (!valid) {
            throw new IllegalStateException("Cannot encode an invalid market tooltip price response.");
        }
        MarketPacketCodec.writeCommodityKey(buffer, commodityKey);
        buffer.writeLong(marketDay);
        buffer.writeLong(price);
    }
}
