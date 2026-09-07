package lisbam.pastoraleconomy.network.message;

import io.netty.buffer.ByteBuf;
import lisbam.pastoraleconomy.merchant.EmeraldTradeAction;
import lisbam.pastoraleconomy.merchant.MerchantTradeService;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.UUID;

/**
 * Bounded C2S spot-market request.  It contains no price, balance, fee, or
 * inventory state; all settlement values are rebuilt by the logical server.
 */
public final class EmeraldTradeRequestMessage implements IMessage {
    private UUID merchantId;
    private int windowId;
    private long expectedWorldDay;
    private boolean buy;
    private int quantity;
    private int requestId;
    private boolean valid;

    public EmeraldTradeRequestMessage() {
    }

    public EmeraldTradeRequestMessage(UUID merchantId, int windowId, long expectedWorldDay,
                                      EmeraldTradeAction action, int quantity, int requestId) {
        this.merchantId = merchantId;
        this.windowId = windowId;
        this.expectedWorldDay = expectedWorldDay;
        this.buy = action == EmeraldTradeAction.BUY;
        this.quantity = quantity;
        this.requestId = requestId;
        this.valid = merchantId != null && action != null && expectedWorldDay >= 0L && requestId > 0
                && quantity > 0 && quantity <= MerchantTradeService.MAX_REQUEST_QUANTITY;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        valid = false;
        if (buffer.readableBytes() < 16 + 4 + 8 + 1 + 4 + 4) {
            return;
        }
        merchantId = new UUID(buffer.readLong(), buffer.readLong());
        windowId = buffer.readInt();
        expectedWorldDay = buffer.readLong();
        buy = buffer.readBoolean();
        quantity = buffer.readInt();
        requestId = buffer.readInt();
        valid = expectedWorldDay >= 0L && requestId > 0 && quantity > 0
                && quantity <= MerchantTradeService.MAX_REQUEST_QUANTITY;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (!valid) {
            throw new IllegalStateException("Cannot encode invalid emerald trade request.");
        }
        buffer.writeLong(merchantId.getMostSignificantBits());
        buffer.writeLong(merchantId.getLeastSignificantBits());
        buffer.writeInt(windowId);
        buffer.writeLong(expectedWorldDay);
        buffer.writeBoolean(buy);
        buffer.writeInt(quantity);
        buffer.writeInt(requestId);
    }

    public MerchantTradeService.EmeraldTradeRequest toDomainRequest() {
        return new MerchantTradeService.EmeraldTradeRequest(merchantId, windowId, expectedWorldDay,
                buy ? EmeraldTradeAction.BUY : EmeraldTradeAction.SELL, quantity, requestId);
    }
}
