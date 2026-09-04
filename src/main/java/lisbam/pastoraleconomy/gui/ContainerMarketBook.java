package lisbam.pastoraleconomy.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/**
 * Empty server counterpart required by Forge's 1.12.2 open-GUI protocol.
 * The market book has no inventory, slots, or server-side GUI state.
 */
public final class ContainerMarketBook extends Container {
    private static final long REQUEST_INTERVAL_TICKS = 2L;
    private long nextRequestTick = Long.MIN_VALUE;

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    /** Limits one open book session to a bounded request rate without global state. */
    public boolean acceptMarketRequest(long currentTick) {
        if (currentTick < nextRequestTick) {
            return false;
        }
        nextRequestTick = currentTick > Long.MAX_VALUE - REQUEST_INTERVAL_TICKS
                ? Long.MAX_VALUE : currentTick + REQUEST_INTERVAL_TICKS;
        return true;
    }
}
