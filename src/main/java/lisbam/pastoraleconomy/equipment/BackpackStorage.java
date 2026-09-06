package lisbam.pastoraleconomy.equipment;

import lisbam.pastoraleconomy.item.ItemBackpack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;

/** Bounded ItemStack-NBT storage owned by one backpack item. */
public final class BackpackStorage {
    static final String DATA_TAG = "LisBamBackpack";
    static final String ITEMS_TAG = "Items";
    static final String SLOT_TAG = "Slot";

    private BackpackStorage() {
    }

    public static int getCapacity(ItemStack backpackStack) {
        ItemBackpack backpack = ItemBackpack.getBackpack(backpackStack);
        return backpack == null ? 0 : backpack.getCapacity();
    }

    public static NonNullList<ItemStack> read(ItemStack backpackStack) {
        int capacity = getCapacity(backpackStack);
        NonNullList<ItemStack> contents = NonNullList.withSize(capacity, ItemStack.EMPTY);
        if (capacity == 0 || backpackStack.getTagCompound() == null
                || !backpackStack.getTagCompound().hasKey(DATA_TAG, 10)) {
            return contents;
        }
        NBTTagList serialized = backpackStack.getTagCompound().getCompoundTag(DATA_TAG).getTagList(ITEMS_TAG, 10);
        boolean[] occupied = new boolean[capacity];
        for (int index = 0; index < serialized.tagCount(); index++) {
            NBTTagCompound entry = serialized.getCompoundTagAt(index);
            int slot = entry.getByte(SLOT_TAG) & 255;
            ItemStack stored = new ItemStack(entry);
            if (slot < capacity && !occupied[slot] && !stored.isEmpty() && !ItemBackpack.isBackpack(stored)) {
                stored.setCount(Math.min(stored.getCount(), stored.getMaxStackSize()));
                contents.set(slot, stored);
                occupied[slot] = true;
            }
        }
        return contents;
    }

    public static void write(ItemStack backpackStack, NonNullList<ItemStack> contents) {
        int capacity = getCapacity(backpackStack);
        if (capacity == 0 || contents == null || contents.size() != capacity) {
            throw new IllegalArgumentException("Backpack contents do not match the backpack tier.");
        }
        NBTTagList serialized = new NBTTagList();
        for (int slot = 0; slot < capacity; slot++) {
            ItemStack stored = contents.get(slot);
            if (stored == null || stored.isEmpty() || ItemBackpack.isBackpack(stored)) {
                continue;
            }
            NBTTagCompound entry = stored.writeToNBT(new NBTTagCompound());
            entry.setByte(SLOT_TAG, (byte) slot);
            serialized.appendTag(entry);
        }
        NBTTagCompound data = backpackStack.getOrCreateSubCompound(DATA_TAG);
        data.setTag(ITEMS_TAG, serialized);
    }

    public static boolean isEmpty(ItemStack backpackStack) {
        for (ItemStack stored : read(backpackStack)) {
            if (!stored.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
