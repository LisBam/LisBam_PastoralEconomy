package lisbam.pastoraleconomy.equipment;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Prevents recursive backpack storage through every Container click path. */
public final class SlotBackpackStorage extends Slot {
    public SlotBackpackStorage(BackpackInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return inventory.isItemValidForSlot(getSlotIndex(), stack);
    }
}
