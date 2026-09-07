package lisbam.pastoraleconomy.equipment;

import lisbam.pastoraleconomy.item.ItemBackpack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/** One 27-slot view onto the equipped backpack's server-owned NBT contents. */
public final class BackpackInventory implements IInventory {
    private final EntityPlayer player;
    private final ItemBackpack expectedBackpack;
    private final NonNullList<ItemStack> contents;
    private int page;

    public BackpackInventory(EntityPlayer player) {
        this.player = player;
        ItemStack equipped = ShoulderEquipmentService.getShoulderStack(player);
        this.expectedBackpack = ItemBackpack.getBackpack(equipped);
        if (expectedBackpack == null) {
            throw new IllegalArgumentException("A backpack inventory requires an equipped backpack.");
        }
        this.contents = BackpackStorage.read(equipped);
    }

    public int getPageCount() {
        return expectedBackpack.getPageCount();
    }

    public int getPage() {
        return page;
    }

    public boolean setPage(int requestedPage) {
        if (requestedPage < 0 || requestedPage >= getPageCount()) {
            return false;
        }
        page = requestedPage;
        return true;
    }

    public boolean hasExpectedBackpack() {
        return isExpectedBackpack(ShoulderEquipmentService.getShoulderStack(player), expectedBackpack);
    }

    /** Stack identity is intentionally irrelevant because shoulder state is copied for synchronization. */
    static boolean isExpectedBackpack(ItemStack equipped, ItemBackpack expectedBackpack) {
        // PlayerData and SyncShoulderEquipmentMessage deliberately store value
        // copies. Object identity therefore changes after every authoritative
        // NBT write or S2C snapshot while this Container remains open.
        return expectedBackpack != null && !equipped.isEmpty() && equipped.getItem() == expectedBackpack;
    }

    public ItemStack getEquippedBackpack() {
        return hasExpectedBackpack() ? ShoulderEquipmentService.getShoulderStack(player) : ItemStack.EMPTY;
    }

    @Override
    public int getSizeInventory() {
        return ItemBackpack.PAGE_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getSizeInventory(); slot++) {
            if (!getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        int storageIndex = getStorageIndex(index);
        if (storageIndex < 0 || !hasExpectedBackpack()) {
            return ItemStack.EMPTY;
        }
        return contents.get(storageIndex);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        int storageIndex = getStorageIndex(index);
        if (storageIndex < 0 || !hasExpectedBackpack() || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = contents.get(storageIndex).splitStack(count);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        int storageIndex = getStorageIndex(index);
        if (storageIndex < 0 || !hasExpectedBackpack()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = contents.get(storageIndex);
        if (!result.isEmpty()) {
            contents.set(storageIndex, ItemStack.EMPTY);
            markDirty();
        }
        return result;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        int storageIndex = getStorageIndex(index);
        ItemStack backpack = getEquippedBackpack();
        if (storageIndex < 0 || backpack.isEmpty()) {
            return;
        }
        ItemStack stored = stack == null ? ItemStack.EMPTY : stack;
        if (!stored.isEmpty() && (!isItemValidForSlot(index, stored) || stored.getCount() > stored.getMaxStackSize())) {
            return;
        }
        contents.set(storageIndex, stored.isEmpty() ? ItemStack.EMPTY : stored.copy());
        markDirty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        ItemStack backpack = getEquippedBackpack();
        if (!backpack.isEmpty()) {
            BackpackStorage.write(backpack, contents);
            ShoulderEquipmentService.setShoulderStack(player, backpack);
        }
        player.inventory.markDirty();
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer candidate) {
        return candidate == player && !player.isDead && hasExpectedBackpack();
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return getStorageIndex(index) >= 0 && stack != null && !stack.isEmpty() && !ItemBackpack.isBackpack(stack);
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        ItemStack backpack = getEquippedBackpack();
        if (backpack.isEmpty()) {
            return;
        }
        for (int index = 0; index < contents.size(); index++) {
            contents.set(index, ItemStack.EMPTY);
        }
        markDirty();
    }

    @Override
    public String getName() {
        return expectedBackpack.getUnlocalizedName() + ".name";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation(getName());
    }

    private int getStorageIndex(int localIndex) {
        if (localIndex < 0 || localIndex >= ItemBackpack.PAGE_SIZE) {
            return -1;
        }
        int storageIndex = page * ItemBackpack.PAGE_SIZE + localIndex;
        return storageIndex < expectedBackpack.getCapacity() ? storageIndex : -1;
    }

}
