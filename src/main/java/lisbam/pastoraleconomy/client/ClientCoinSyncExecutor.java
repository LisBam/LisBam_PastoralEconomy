package lisbam.pastoraleconomy.client;

import net.minecraft.client.Minecraft;

/** Schedules the Netty-delivered S2C value onto the 1.12.2 client main thread. */
public final class ClientCoinSyncExecutor {
    private ClientCoinSyncExecutor() {
    }

    public static void acceptServerBalance(final long balance) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientPlayerState.acceptCoins(balance);
            }
        });
    }
}
