package lisbam.pastoraleconomy.gui;

import lisbam.pastoraleconomy.entity.EntityMerchant;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import java.util.UUID;

/** Slotless server container that binds the GUI to one live merchant entity. */
public final class ContainerMerchantTrade extends Container {
    private final UUID merchantId;
    private final int entityId;
    private boolean committing;
    private int lastRequestId;

    public ContainerMerchantTrade(EntityMerchant merchant) {
        merchantId = merchant.getMerchantId();
        entityId = merchant.getEntityId();
    }

    /**
     * Client-side lifecycle counterpart for Forge's OpenGui protocol. The
     * authoritative server always uses the entity-bound constructor above.
     */
    public ContainerMerchantTrade(int entityId) {
        merchantId = null;
        this.entityId = entityId;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        Entity entity = player.world.getEntityByID(entityId);
        return merchantId != null && entity instanceof EntityMerchant && !entity.isDead
                && merchantId.equals(((EntityMerchant) entity).getMerchantId())
                && player.getDistanceSq(entity) <= 64.0D;
    }

    public UUID getMerchantId() { return merchantId; }
    public int getEntityId() { return entityId; }
    public boolean isCommitting() { return committing; }
    public void beginCommit() { committing = true; }
    public void endCommit() { committing = false; }
    public boolean acceptRequestId(int requestId) {
        if (requestId <= 0 || requestId <= lastRequestId) {
            return false;
        }
        lastRequestId = requestId;
        return true;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        committing = false;
        Entity entity = playerIn.world.getEntityByID(entityId);
        if (merchantId != null && entity instanceof EntityMerchant
                && merchantId.equals(((EntityMerchant) entity).getMerchantId())) {
            ((EntityMerchant) entity).endTrading(playerIn);
        }
    }
}
