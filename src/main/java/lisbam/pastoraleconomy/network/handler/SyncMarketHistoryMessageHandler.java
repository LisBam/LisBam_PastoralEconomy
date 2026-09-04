package lisbam.pastoraleconomy.network.handler;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.market.MarketHistorySnapshot;
import lisbam.pastoraleconomy.network.message.SyncMarketHistoryMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Common-side S2C bridge; ClientProxy schedules the actual cache mutation. */
public final class SyncMarketHistoryMessageHandler implements IMessageHandler<SyncMarketHistoryMessage, IMessage> {
    @Override
    public IMessage onMessage(SyncMarketHistoryMessage message, MessageContext context) {
        MarketHistorySnapshot snapshot = message.getSnapshot();
        if (snapshot != null) {
            LisBamPastoralEconomy.proxy.handleMarketHistorySync(snapshot);
        }
        return null;
    }
}
