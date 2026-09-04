package lisbam.pastoraleconomy.network.handler;

import lisbam.pastoraleconomy.network.message.TransportStationActionMessage;
import lisbam.pastoraleconomy.transport.TransportAction;
import lisbam.pastoraleconomy.transport.TransportService;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Schedules every C2S transport mutation onto the logical server main thread. */
public final class TransportStationActionMessageHandler
        implements IMessageHandler<TransportStationActionMessage, IMessage> {
    @Override
    public IMessage onMessage(final TransportStationActionMessage message, MessageContext context) {
        if (!message.isValid() || context.getServerHandler() == null) {
            return null;
        }
        final EntityPlayerMP player = context.getServerHandler().player;
        player.getServerWorld().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                TransportAction action = message.getAction();
                if (action == TransportAction.REFRESH) {
                    TransportService.handleRefresh(player, message.getStationId());
                } else if (action == TransportAction.CONNECT) {
                    TransportService.handleConnect(player, message.getStationId());
                } else if (action == TransportAction.REMOVE) {
                    TransportService.handleRemove(player, message.getStationId());
                } else if (action == TransportAction.RENAME) {
                    TransportService.handleRename(player, message.getStationId(), message.getAlias());
                } else if (action == TransportAction.FIND_VILLAGE) {
                    TransportService.handleFindVillage(player, message.getStationId());
                } else if (action == TransportAction.CONNECT_VILLAGE) {
                    TransportService.handleConnectVillage(player, message.getStationId());
                } else if (action == TransportAction.TRAVEL) {
                    TransportService.handleTravel(player, message.getStationId());
                }
            }
        });
        return null;
    }
}
