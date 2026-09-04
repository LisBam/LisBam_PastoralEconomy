package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.market.MarketHistorySnapshot;
import net.minecraft.client.Minecraft;

/** Schedules Netty-delivered market display snapshots onto the client main thread. */
public final class ClientMarketSyncExecutor {
    private ClientMarketSyncExecutor() {
    }

    public static void acceptServerSnapshot(final MarketHistorySnapshot snapshot) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientMarketState.acceptSnapshot(snapshot);
            }
        });
    }
}
