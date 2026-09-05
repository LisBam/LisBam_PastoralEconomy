package lisbam.pastoraleconomy.core;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Values injected into Item's Forge 1.12.2 enchanting-table hook. */
public final class EnchantingCompatibilityHooks {
    private EnchantingCompatibilityHooks() {
    }

    public static int getItemEnchantability(Item item, ItemStack stack, int vanillaValue) {
        if (item == Items.SHEARS) {
            return Item.ToolMaterial.IRON.getEnchantability();
        }
        if (item == Items.WOODEN_HOE) {
            return Item.ToolMaterial.WOOD.getEnchantability();
        }
        if (item == Items.STONE_HOE) {
            return Item.ToolMaterial.STONE.getEnchantability();
        }
        if (item == Items.IRON_HOE) {
            return Item.ToolMaterial.IRON.getEnchantability();
        }
        if (item == Items.GOLDEN_HOE) {
            return Item.ToolMaterial.GOLD.getEnchantability();
        }
        if (item == Items.DIAMOND_HOE) {
            return Item.ToolMaterial.DIAMOND.getEnchantability();
        }
        return vanillaValue;
    }
}
