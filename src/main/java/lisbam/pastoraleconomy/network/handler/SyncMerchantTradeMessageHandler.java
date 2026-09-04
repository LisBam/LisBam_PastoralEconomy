package lisbam.pastoraleconomy.network.handler;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.network.message.SyncMerchantTradeMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class SyncMerchantTradeMessageHandler implements IMessageHandler<SyncMerchantTradeMessage, IMessage> {
    @Override
    public IMessage onMessage(SyncMerchantTradeMessage message, MessageContext context) {
        if (message.getSnapshot() != null) {
            LisBamPastoralEconomy.proxy.handleMerchantTradeSync(message.getSnapshot());
        }
        return null;
    }
}
