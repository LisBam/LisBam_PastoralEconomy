package lisbam.pastoraleconomy.gui;

import lisbam.pastoraleconomy.equipment.BackpackInventory;
import lisbam.pastoraleconomy.equipment.SlotBackpackStorage;
import lisbam.pastoraleconomy.item.ItemBackpack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Server-authoritative three-row page over the currently equipped backpack. */
public final class ContainerBackpack extends Container {
    private static final int STORAGE_SLOTS = ItemBackpack.PAGE_SIZE;
    private static final int PLAYER_SLOTS = 36;

    private final BackpackInventory backpackInventory;
    private final EntityPlayer player;

    public ContainerBackpack(InventoryPlayer playerInventory) {
        this.player = playerInventory.player;
        this.backpackInventory = new BackpackInventory(player);
        backpackInventory.openInventory(player);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int index = column + row * 9;
                addSlotToContainer(new SlotBackpackStorage(backpackInventory, index,
                        8 + column * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 85 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(playerInventory, column, 8 + column * 18, 143));
        }
    }

    public int getPage() {
        return backpackInventory.getPage();
    }

    public int getPageCount() {
        return backpackInventory.getPageCount();
    }

    public String getBackpackName() {
        ItemStack backpack = backpackInventory.getEquippedBackpack();
        return backpack.isEmpty() ? "" : backpack.getDisplayName();
    }

    public boolean setPage(int page) {
        return backpackInventory.setPage(page);
    }

    @Override
    public boolean enchantItem(EntityPlayer candidate, int id) {
        return candidate == player && canInteractWith(candidate) && setPage(id);
    }

    @Override
    public boolean canInteractWith(EntityPlayer candidate) {
        return backpackInventory.isUsableByPlayer(candidate);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        if (index < 0 || index >= inventorySlots.size()) {
            return ItemStack.EMPTY;
        }
        Slot source = inventorySlots.get(index);
        if (source == null || !source.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = source.getStack();
        ItemStack original = sourceStack.copy();
        boolean moved;
        if (index < STORAGE_SLOTS) {
            moved = mergeItemStack(sourceStack, STORAGE_SLOTS, STORAGE_SLOTS + PLAYER_SLOTS, true);
        } else {
            moved = mergeItemStack(sourceStack, 0, STORAGE_SLOTS, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (sourceStack.isEmpty()) {
            source.putStack(ItemStack.EMPTY);
        } else {
            source.onSlotChanged();
        }
        if (sourceStack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        source.onTake(playerIn, sourceStack);
        return original;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        backpackInventory.closeInventory(playerIn);
    }
}
