package lisbam.pastoraleconomy.network.handler;

import lisbam.pastoraleconomy.merchant.MerchantTradeService;
import lisbam.pastoraleconomy.network.message.EmeraldTradeRequestMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Schedules the untrusted spot-market request onto the logical server thread. */
public final class EmeraldTradeRequestMessageHandler
        implements IMessageHandler<EmeraldTradeRequestMessage, IMessage> {
    @Override
    public IMessage onMessage(final EmeraldTradeRequestMessage message, MessageContext context) {
        if (!message.isValid() || context.getServerHandler() == null) {
            return null;
        }
        final EntityPlayerMP player = context.getServerHandler().player;
        player.getServerWorld().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                MerchantTradeService.handleEmeraldTransaction(player, message.toDomainRequest());
            }
        });
        return null;
    }
}
