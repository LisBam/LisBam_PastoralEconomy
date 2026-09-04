package lisbam.pastoraleconomy.network.handler;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.network.message.SyncTransportStateMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Common S2C bridge; ClientProxy supplies the physical-client main-thread work. */
public final class SyncTransportStateMessageHandler implements IMessageHandler<SyncTransportStateMessage, IMessage> {
    @Override
    public IMessage onMessage(SyncTransportStateMessage message, MessageContext context) {
        if (message.getSnapshot() != null) {
            LisBamPastoralEconomy.proxy.handleTransportStateSync(message.getSnapshot());
        }
        return null;
    }
}
