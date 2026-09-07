package lisbam.pastoraleconomy.gui;

import lisbam.pastoraleconomy.tile.TileShippingBox;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Standard three-row chest container backed by the shipping-box TileEntity. */
public final class ContainerShippingBox extends Container {
    private static final int BOX_SLOT_COUNT = TileShippingBox.SLOT_COUNT;
    private static final int PLAYER_FIRST_SLOT = BOX_SLOT_COUNT;
    private static final int PLAYER_LAST_SLOT = PLAYER_FIRST_SLOT + 36;

    private final TileShippingBox box;

    public ContainerShippingBox(InventoryPlayer playerInventory, TileShippingBox box) {
        this.box = box;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(box, column + row * 9, 8 + column * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return box.isUsableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = ItemStack.EMPTY;
        if (index < 0 || index >= inventorySlots.size()) {
            return result;
        }
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return result;
        }
        ItemStack source = slot.getStack();
        result = source.copy();
        if (index < BOX_SLOT_COUNT) {
            if (!mergeItemStack(source, PLAYER_FIRST_SLOT, PLAYER_LAST_SLOT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!mergeItemStack(source, 0, BOX_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        if (source.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, source);
        return result;
    }
}
