package lisbam.pastoraleconomy.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

import javax.annotation.Nullable;

/**
 * Slotless server counterpart required by Forge's 1.12.2 open-GUI protocol.
 * It marks the server-side open-book session used to validate display requests.
 * Request timing stays in the already-scheduled network task: a slotless
 * container has no inventory delta that guarantees deferred work is observed.
 */
public final class ContainerMarketBook extends Container {
    public ContainerMarketBook() {
        this(null);
    }

    public ContainerMarketBook(@Nullable EntityPlayerMP owner) {
        // The current owner is authoritative through EntityPlayerMP#openContainer.
        // Keep this constructor signature for the Forge GUI factory.
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

}
