package lisbam.pastoraleconomy.gui;

import lisbam.pastoraleconomy.network.handler.RequestMarketHistoryMessageHandler;
import lisbam.pastoraleconomy.network.message.RequestMarketHistoryMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

import javax.annotation.Nullable;

/**
 * Slotless server counterpart required by Forge's 1.12.2 open-GUI protocol.
 * It owns only the bounded scheduling of display requests, never market state.
 */
public final class ContainerMarketBook extends Container {
    private static final long REQUEST_INTERVAL_TICKS = 2L;
    @Nullable
    private final EntityPlayerMP owner;
    private long nextRequestTick = Long.MIN_VALUE;
    @Nullable
    private RequestMarketHistoryMessage pendingRequest;

    public ContainerMarketBook() {
        this(null);
    }

    public ContainerMarketBook(@Nullable EntityPlayerMP owner) {
        this.owner = owner;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    /**
     * Coalesces requests received during the cooldown. Dropping them would
     * leave a display-only GUI waiting forever because it has no failure packet.
     */
    public void queueMarketRequest(RequestMarketHistoryMessage request) {
        pendingRequest = request;
    }

    /** Returns the newest queued request once the per-book rate limit allows it. */
    @Nullable
    public RequestMarketHistoryMessage pollMarketRequest(long currentTick) {
        if (pendingRequest == null || currentTick < nextRequestTick) {
            return null;
        }
        nextRequestTick = currentTick > Long.MAX_VALUE - REQUEST_INTERVAL_TICKS
                ? Long.MAX_VALUE : currentTick + REQUEST_INTERVAL_TICKS;
        RequestMarketHistoryMessage request = pendingRequest;
        pendingRequest = null;
        return request;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (owner == null || owner.connection == null || owner.world == null || owner.world.isRemote) {
            return;
        }
        RequestMarketHistoryMessage request = pollMarketRequest(owner.getServerWorld().getTotalWorldTime());
        if (request != null) {
            RequestMarketHistoryMessageHandler.handleAcceptedRequest(owner, request);
        }
    }
}
