package lisbam.pastoraleconomy.network.message;

import io.netty.buffer.ByteBuf;
import lisbam.pastoraleconomy.merchant.MerchantTradeService;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.UUID;

/** C2S request carrying only session/slot/quantity; price and output remain server-derived. */
public final class MerchantTradeRequestMessage implements IMessage {
    private UUID merchantId;
    private int windowId;
    private long expectedWorldDay;
    private boolean buy;
    private int slot;
    private int quantity;
    private int requestId;
    private boolean valid;

    public MerchantTradeRequestMessage() {
    }

    public MerchantTradeRequestMessage(UUID merchantId, int windowId, long expectedWorldDay, boolean buy, int slot,
                                       int quantity, int requestId) {
        this.merchantId = merchantId;
        this.windowId = windowId;
        this.expectedWorldDay = expectedWorldDay;
        this.buy = buy;
        this.slot = slot;
        this.quantity = quantity;
        this.requestId = requestId;
        this.valid = merchantId != null && expectedWorldDay >= 0L && requestId > 0
                && slot >= 0 && slot < (buy ? 10 : 6) && quantity > 0 && quantity <= 4096;
    }

    public boolean isValid() { return valid; }
    public UUID getMerchantId() { return merchantId; }
    public int getWindowId() { return windowId; }
    public long getExpectedWorldDay() { return expectedWorldDay; }
    public boolean isBuy() { return buy; }
    public int getSlot() { return slot; }
    public int getQuantity() { return quantity; }
    public int getRequestId() { return requestId; }

    @Override
    public void fromBytes(ByteBuf buffer) {
        valid = false;
        if (buffer.readableBytes() < 16 + 4 + 8 + 1 + 4 + 4 + 4) {
            return;
        }
        merchantId = new UUID(buffer.readLong(), buffer.readLong());
        windowId = buffer.readInt();
        expectedWorldDay = buffer.readLong();
        buy = buffer.readBoolean();
        slot = buffer.readInt();
        quantity = buffer.readInt();
        requestId = buffer.readInt();
        valid = merchantId != null && expectedWorldDay >= 0L && requestId > 0
                && slot >= 0 && slot < (buy ? 10 : 6) && quantity > 0 && quantity <= 4096;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (!valid) {
            throw new IllegalStateException("Cannot encode invalid merchant trade request.");
        }
        buffer.writeLong(merchantId.getMostSignificantBits());
        buffer.writeLong(merchantId.getLeastSignificantBits());
        buffer.writeInt(windowId);
        buffer.writeLong(expectedWorldDay);
        buffer.writeBoolean(buy);
        buffer.writeInt(slot);
        buffer.writeInt(quantity);
        buffer.writeInt(requestId);
    }

    public MerchantTradeService.MerchantTradeRequest toDomainRequest() {
        return new MerchantTradeService.MerchantTradeRequest(merchantId, windowId, expectedWorldDay, buy, slot, quantity, requestId);
    }
}
