package lisbam.pastoraleconomy.equipment;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/** One-slot IInventory adapter backed by the existing player Capability. */
final class ShoulderEquipmentInventory implements IInventory {
    private final EntityPlayer player;

    ShoulderEquipmentInventory(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return getStackInSlot(0).isEmpty();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index == 0 ? ShoulderEquipmentService.getShoulderStack(player) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = getStackInSlot(index);
        if (stack.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.splitStack(count);
        if (stack.isEmpty()) {
            ShoulderEquipmentService.setShoulderStack(player, ItemStack.EMPTY);
        }
        return result;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = getStackInSlot(index);
        if (!stack.isEmpty()) {
            ShoulderEquipmentService.setShoulderStack(player, ItemStack.EMPTY);
        }
        return stack;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index == 0) {
            ShoulderEquipmentService.setShoulderStack(player, stack);
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public void markDirty() {
        // Player capability data is serialized with its owning player entity.
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer candidate) {
        return candidate == player && !player.isDead;
    }

    @Override
    public void openInventory(EntityPlayer player) {
        // No transient state.
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        // No transient state.
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == 0 && stack.getItem() == net.minecraft.init.Items.ELYTRA;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
        // No integer fields.
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        ShoulderEquipmentService.setShoulderStack(player, ItemStack.EMPTY);
    }

    @Override
    public String getName() {
        return "container.lisbam_pastoral_economy.shoulder";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation(getName());
    }
}
