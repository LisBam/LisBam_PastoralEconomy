package lisbam.pastoraleconomy.network.handler;

import lisbam.pastoraleconomy.merchant.MerchantTradeService;
import lisbam.pastoraleconomy.network.message.MerchantTradeRequestMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MerchantTradeRequestMessageHandler implements IMessageHandler<MerchantTradeRequestMessage, IMessage> {
    @Override
    public IMessage onMessage(final MerchantTradeRequestMessage message, MessageContext context) {
        if (!message.isValid() || context.getServerHandler() == null) {
            return null;
        }
        final EntityPlayerMP player = context.getServerHandler().player;
        player.getServerWorld().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                MerchantTradeService.handleTransaction(player, message.toDomainRequest());
            }
        });
        return null;
    }
}
