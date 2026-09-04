package lisbam.pastoraleconomy.network.handler;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.network.message.SyncCoinsMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-loadable packet bridge. Client scheduling and cache writes remain in
 * ClientProxy and client classes, so dedicated servers never resolve client APIs.
 */
public final class SyncCoinsMessageHandler implements IMessageHandler<SyncCoinsMessage, IMessage> {
    @Override
    public IMessage onMessage(SyncCoinsMessage message, MessageContext context) {
        LisBamPastoralEconomy.proxy.handleCoinsSync(message.getBalance());
        return null;
    }
}
