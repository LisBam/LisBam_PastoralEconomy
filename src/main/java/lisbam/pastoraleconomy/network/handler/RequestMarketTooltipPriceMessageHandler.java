package lisbam.pastoraleconomy.network.handler;

import lisbam.pastoraleconomy.market.MarketCommodity;
import lisbam.pastoraleconomy.market.MarketService;
import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.network.message.RequestMarketTooltipPriceMessage;
import lisbam.pastoraleconomy.network.message.SyncMarketTooltipPriceMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Validates a market key before returning its server-frozen current purchase price. */
public final class RequestMarketTooltipPriceMessageHandler
        implements IMessageHandler<RequestMarketTooltipPriceMessage, IMessage> {
    @Override
    public IMessage onMessage(final RequestMarketTooltipPriceMessage message, MessageContext context) {
        if (!message.isValid() || context.getServerHandler() == null || context.getServerHandler().player == null) {
            return null;
        }
        final EntityPlayerMP player = context.getServerHandler().player;
        player.getServerWorld().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                handleOnServerThread(player, message.getCommodityKey());
            }
        });
        return null;
    }

    private static void handleOnServerThread(EntityPlayerMP player, String commodityKey) {
        if (player.connection == null || player.world == null || player.world.isRemote) {
            return;
        }
        MarketCommodity requested = MarketService.getCommodity(commodityKey);
        if (!MarketService.isSellCommodity(requested)) {
            return;
        }
        long currentDay = MarketService.getCurrentMarketDay(player.getServerWorld());
        long currentPrice = MarketService.getCurrentPrice(player.getServerWorld(), requested.getKey());
        ModNetwork.CHANNEL.sendTo(new SyncMarketTooltipPriceMessage(requested.getKey(), currentDay, currentPrice), player);
    }

}
