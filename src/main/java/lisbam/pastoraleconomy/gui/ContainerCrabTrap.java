package lisbam.pastoraleconomy.gui;

import lisbam.pastoraleconomy.tile.TileCrabTrap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Authoritative general 20-slot crab-trap container with safe shift-clicking. */
public final class ContainerCrabTrap extends Container {
    private static final int TRAP_SLOT_COUNT = TileCrabTrap.SLOT_COUNT;
    private static final int PLAYER_FIRST_SLOT = TRAP_SLOT_COUNT;
    private static final int PLAYER_MAIN_END = PLAYER_FIRST_SLOT + 27;
    private static final int PLAYER_LAST_SLOT = PLAYER_FIRST_SLOT + 36;

    private final TileCrabTrap trap;
    private final int[] lastFields = new int[6];
    private boolean fieldsInitialized;

    public ContainerCrabTrap(InventoryPlayer playerInventory, TileCrabTrap trap) {
        this.trap = trap;
        addSlotToContainer(new Slot(trap, TileCrabTrap.ROD_SLOT, 26, 19));
        addSlotToContainer(new Slot(trap, TileCrabTrap.BAIT_SLOT, 62, 19));

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = TileCrabTrap.FIRST_HARVEST_SLOT + row * 9 + column;
                addSlotToContainer(new Slot(trap, slot, 8 + column * 18, 54 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 103 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(playerInventory, column, 8 + column * 18, 161));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return trap.isUsableByPlayer(player);
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendAllWindowProperties(this, trap);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (int field = 0; field < trap.getFieldCount(); field++) {
            int value = trap.getField(field);
            if (!fieldsInitialized || lastFields[field] != value) {
                for (IContainerListener listener : listeners) {
                    listener.sendWindowProperty(this, field, value);
                }
                lastFields[field] = value;
            }
        }
        fieldsInitialized = true;
    }

    @Override
    public void updateProgressBar(int id, int data) {
        trap.setField(id, data);
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
        if (index < TRAP_SLOT_COUNT) {
            if (!mergeItemStack(source, PLAYER_FIRST_SLOT, PLAYER_LAST_SLOT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!mergeItemStack(source, 0, TRAP_SLOT_COUNT, false)) {
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
