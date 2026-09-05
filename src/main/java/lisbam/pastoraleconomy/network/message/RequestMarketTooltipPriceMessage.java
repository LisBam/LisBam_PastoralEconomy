package lisbam.pastoraleconomy.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Bounded C2S request for the current price of one sellable inventory commodity. */
public final class RequestMarketTooltipPriceMessage implements IMessage {
    private String commodityKey;
    private boolean valid;

    public RequestMarketTooltipPriceMessage() {
    }

    public RequestMarketTooltipPriceMessage(String commodityKey) {
        this.commodityKey = commodityKey;
        this.valid = commodityKey != null && !commodityKey.isEmpty();
    }

    public String getCommodityKey() {
        return commodityKey;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        commodityKey = MarketPacketCodec.readCommodityKey(buffer);
        valid = commodityKey != null;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        MarketPacketCodec.writeCommodityKey(buffer, commodityKey);
    }
}
