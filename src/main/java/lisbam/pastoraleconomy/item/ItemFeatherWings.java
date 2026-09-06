package lisbam.pastoraleconomy.item;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;

/**
 * A shoulder-only flight item. Flight state, hunger and durability are owned
 * by {@code FeatherWingsFlightEventHandler}; this item only declares the
 * stable stack, repair and enchantment boundaries.
 */
public final class ItemFeatherWings extends ItemElytra {
    public static final int MAX_DAMAGE = 500;

    ItemFeatherWings() {
        setRegistryName(LisBamPastoralEconomy.MODID, "feather_wings");
        setUnlocalizedName(LisBamPastoralEconomy.MODID + ".feather_wings");
        setCreativeTab(ModItems.CREATIVE_TAB);
        setMaxStackSize(1);
        setMaxDamage(MAX_DAMAGE);
    }

    @Override
    public int getItemEnchantability() {
        // The enchanting table may only offer Unbreaking; Mending and curses
        // remain book/anvil routes just like their vanilla definitions.
        return 1;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        // Enchantment#canApply delegates to this hook in 1.12.2, including
        // when a treasure-enchantment book is combined in an anvil.  The
        // treasure flag still keeps Mending and both curses out of the table.
        return enchantment == Enchantments.UNBREAKING
                || enchantment == Enchantments.MENDING
                || enchantment == Enchantments.BINDING_CURSE
                || enchantment == Enchantments.VANISHING_CURSE;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return !toRepair.isEmpty() && repair.getItem() == Items.FEATHER;
    }

    public static boolean isFeatherWings(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemFeatherWings;
    }
}
