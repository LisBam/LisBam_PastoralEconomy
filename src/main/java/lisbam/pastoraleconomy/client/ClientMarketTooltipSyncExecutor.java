package lisbam.pastoraleconomy.client;

import net.minecraft.client.Minecraft;

/** Schedules server-owned inventory-tooltip prices onto the physical client thread. */
public final class ClientMarketTooltipSyncExecutor {
    private ClientMarketTooltipSyncExecutor() {
    }

    public static void acceptServerPrice(final String commodityKey, final long marketDay, final long price) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientMarketTooltipState.acceptPrice(commodityKey, marketDay, price);
            }
        });
    }
}
