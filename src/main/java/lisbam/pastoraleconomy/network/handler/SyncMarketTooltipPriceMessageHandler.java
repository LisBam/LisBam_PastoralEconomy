package lisbam.pastoraleconomy.network.handler;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.network.message.SyncMarketTooltipPriceMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Common S2C bridge; ClientProxy isolates the physical-client cache update. */
public final class SyncMarketTooltipPriceMessageHandler
        implements IMessageHandler<SyncMarketTooltipPriceMessage, IMessage> {
    @Override
    public IMessage onMessage(SyncMarketTooltipPriceMessage message, MessageContext context) {
        if (message.isValid()) {
            LisBamPastoralEconomy.proxy.handleMarketTooltipPriceSync(
                    message.getCommodityKey(), message.getMarketDay(), message.getPrice());
        }
        return null;
    }
}
