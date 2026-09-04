package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.transport.TransportStateSnapshot;
import net.minecraft.client.Minecraft;

/** Schedules Netty-delivered transport display data on the client main thread. */
public final class ClientTransportStateSyncExecutor {
    private ClientTransportStateSyncExecutor() {
    }

    public static void acceptServerSnapshot(final TransportStateSnapshot snapshot) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientTransportState.accept(snapshot);
            }
        });
    }
}
