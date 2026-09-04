package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.merchant.MerchantTradeSnapshot;
import net.minecraft.client.Minecraft;

/** Schedules merchant display cache writes onto the physical client thread. */
public final class ClientMerchantTradeSyncExecutor {
    private ClientMerchantTradeSyncExecutor() {
    }

    public static void acceptServerSnapshot(final MerchantTradeSnapshot snapshot) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientMerchantTradeState.accept(snapshot);
            }
        });
    }
}
