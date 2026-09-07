package lisbam.pastoraleconomy.network.handler;

import lisbam.pastoraleconomy.market.MarketHistorySnapshot;
import lisbam.pastoraleconomy.market.MarketService;
import lisbam.pastoraleconomy.gui.ContainerMarketBook;
import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.network.message.RequestMarketHistoryMessage;
import lisbam.pastoraleconomy.network.message.SyncMarketHistoryMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;


/** Validates C2S market display requests and reads market state only on the server thread. */
public final class RequestMarketHistoryMessageHandler
        implements IMessageHandler<RequestMarketHistoryMessage, IMessage> {
    @Override
    public IMessage onMessage(final RequestMarketHistoryMessage message, MessageContext context) {
        if (!message.isValid() || context.getServerHandler() == null || context.getServerHandler().player == null) {
            return null;
        }

        final EntityPlayerMP player = context.getServerHandler().player;
        player.getServerWorld().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                handleOnServerThread(player, message);
            }
        });
        return null;
    }

    private static void handleOnServerThread(EntityPlayerMP player, RequestMarketHistoryMessage request) {
        if (player.connection == null || player.world == null || player.world.isRemote) {
            return;
        }

        if (!(player.openContainer instanceof ContainerMarketBook)) {
            return;
        }
        // One request carries one key and at most thirty display points. It is
        // already on the server thread, so answer directly rather than rely on
        // Container#detectAndSendChanges: a slotless container can strand a
        // deferred selection without a later inventory change.
        handleAcceptedRequest(player, request);
    }

    /** Invoked by the open book container on the logical server after rate limiting. */
    public static void handleAcceptedRequest(EntityPlayerMP player, RequestMarketHistoryMessage request) {
        if (player.connection == null || player.world == null || player.world.isRemote
                || !(player.openContainer instanceof ContainerMarketBook)) {
            return;
        }
        if (!MarketService.isHistoryTracked(request.getCommodityKey())) {
            return;
        }

        long cursor = request.getBeforeExclusiveDay();
        if (cursor == -1L) {
            // One bounded response per request keeps opening the book cheap;
            // the client gradually prefetches the remaining catalogue entries.
            MarketHistorySnapshot snapshot = MarketService.getHistorySnapshot(
                    player.getServerWorld(), request.getCommodityKey(), -1L,
                    MarketService.DEFAULT_HISTORY_DAYS, request.getRequestId());
            ModNetwork.CHANNEL.sendTo(new SyncMarketHistoryMessage(snapshot), player);
            return;
        }

        WorldServer playerWorld = player.getServerWorld();
        long currentDay = MarketService.getHistoryCurrentDay(playerWorld, request.getCommodityKey());
        if (cursor > currentDay) {
            return;
        }

        MarketHistorySnapshot snapshot = MarketService.getHistorySnapshot(
                playerWorld,
                request.getCommodityKey(),
                cursor,
                MarketService.DEFAULT_HISTORY_DAYS,
                request.getRequestId()
        );
        ModNetwork.CHANNEL.sendTo(new SyncMarketHistoryMessage(snapshot), player);
    }
}
