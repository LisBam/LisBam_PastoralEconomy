package lisbam.pastoraleconomy.network.handler;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.network.message.SyncShoulderEquipmentMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server-loadable S2C bridge; the proxy isolates client entity lookup and scheduling. */
public final class SyncShoulderEquipmentMessageHandler
        implements IMessageHandler<SyncShoulderEquipmentMessage, IMessage> {
    @Override
    public IMessage onMessage(SyncShoulderEquipmentMessage message, MessageContext context) {
        if (message.isValid()) {
            LisBamPastoralEconomy.proxy.handleShoulderEquipmentSync(message.getEntityId(), message.getStack());
        }
        return null;
    }
}
