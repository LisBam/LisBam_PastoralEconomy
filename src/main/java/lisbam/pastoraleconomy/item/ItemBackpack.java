package lisbam.pastoraleconomy.item;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

/** One non-stackable shoulder backpack tier with an immutable slot capacity. */
public final class ItemBackpack extends Item {
    public static final int PAGE_SIZE = 27;

    private final int capacity;

    ItemBackpack(String path, int capacity) {
        if (capacity <= 0 || capacity % PAGE_SIZE != 0) {
            throw new IllegalArgumentException("Backpack capacity must contain complete 27-slot pages.");
        }
        this.capacity = capacity;
        setRegistryName(LisBamPastoralEconomy.MODID, path);
        setUnlocalizedName(LisBamPastoralEconomy.MODID + "." + path);
        setCreativeTab(ModItems.CREATIVE_TAB);
        setMaxStackSize(1);
    }

    public int getCapacity() {
        return capacity;
    }

    public int getPageCount() {
        return capacity / PAGE_SIZE;
    }

    public static boolean isBackpack(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemBackpack;
    }

    @Nullable
    public static ItemBackpack getBackpack(ItemStack stack) {
        return isBackpack(stack) ? (ItemBackpack) stack.getItem() : null;
    }
}
