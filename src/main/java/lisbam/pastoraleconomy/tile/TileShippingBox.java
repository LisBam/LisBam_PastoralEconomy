package lisbam.pastoraleconomy.tile;

import lisbam.pastoraleconomy.block.ModBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;

/** A chest-sized general inventory that permits hopper input but never hopper extraction. */
public final class TileShippingBox extends TileEntity implements ISidedInventory {
    public static final int SLOT_COUNT = 27;
    private static final int[] ALL_SLOTS = new int[] {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, 26
    };
    private static final String KEY_INVENTORY = "inventory";
    private static final String KEY_SLOT = "slot";

    private NonNullList<ItemStack> inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction) {
        return isValidSlot(index) && isItemValidForSlot(index, stack);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return false;
    }

    @Override
    public int getSizeInventory() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return isValidSlot(index) ? inventory.get(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (!isValidSlot(index) || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = inventory.get(index);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.splitStack(count);
        if (stack.isEmpty()) {
            inventory.set(index, ItemStack.EMPTY);
        }
        markDirty();
        return removed;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (!isValidSlot(index)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = inventory.get(index);
        inventory.set(index, ItemStack.EMPTY);
        if (!removed.isEmpty()) {
            markDirty();
        }
        return removed;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (!isValidSlot(index) || (stack != null && !stack.isEmpty() && !isItemValidForSlot(index, stack))) {
            return;
        }
        setStoredStack(index, stack);
        markDirty();
    }

    private void setStoredStack(int index, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            inventory.set(index, ItemStack.EMPTY);
            return;
        }
        ItemStack stored = stack.copy();
        stored.setCount(Math.min(stored.getCount(), Math.min(getInventoryStackLimit(), stored.getMaxStackSize())));
        inventory.set(index, stored);
    }

    @Override
    public String getName() {
        return "container.lisbam_pastoral_economy.shipping_box";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return world != null && world.getBlockState(pos).getBlock() == ModBlocks.SHIPPING_BOX
                && world.getTileEntity(pos) == this && player.getDistanceSq(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory(EntityPlayer player) {
        // The block has no lid animation or viewer counter.
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        // The block has no lid animation or viewer counter.
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return isValidSlot(index) && stack != null && !stack.isEmpty();
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
        // This inventory has no progress fields.
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        for (int index = 0; index < SLOT_COUNT; index++) {
            inventory.set(index, ItemStack.EMPTY);
        }
        markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList serializedInventory = new NBTTagList();
        for (int index = 0; index < SLOT_COUNT; index++) {
            ItemStack stack = inventory.get(index);
            if (stack.isEmpty()) {
                continue;
            }
            NBTTagCompound stackTag = new NBTTagCompound();
            stackTag.setByte(KEY_SLOT, (byte) index);
            stack.writeToNBT(stackTag);
            serializedInventory.appendTag(stackTag);
        }
        compound.setTag(KEY_INVENTORY, serializedInventory);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        NBTTagList serializedInventory = compound.getTagList(KEY_INVENTORY, 10);
        for (int index = 0; index < serializedInventory.tagCount(); index++) {
            NBTTagCompound stackTag = serializedInventory.getCompoundTagAt(index);
            int slot = stackTag.getByte(KEY_SLOT) & 255;
            if (isValidSlot(slot)) {
                setStoredStack(slot, new ItemStack(stackTag));
            }
        }
    }

    private static boolean isValidSlot(int index) {
        return index >= 0 && index < SLOT_COUNT;
    }
}
