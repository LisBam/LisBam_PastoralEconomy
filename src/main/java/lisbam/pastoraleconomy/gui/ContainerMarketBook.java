package lisbam.pastoraleconomy.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/**
 * Empty server counterpart required by Forge's 1.12.2 open-GUI protocol.
 * The market book has no inventory, slots, or server-side GUI state.
 */
public final class ContainerMarketBook extends Container {
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }
}
