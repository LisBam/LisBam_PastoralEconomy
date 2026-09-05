package lisbam.pastoraleconomy.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.init.Items;

/** Efficiency-compatible enchantment for shears, which are not ItemTool in 1.12.2. */
public final class EnchantmentShearsEfficiency extends EnchantmentPastoral {
    public EnchantmentShearsEfficiency() {
        super("shears_efficiency", Rarity.UNCOMMON, 5,
                new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND},
                new net.minecraft.item.Item[]{Items.SHEARS},
                new Enchantment[]{Enchantments.EFFICIENCY}, false, false, true);
        // Reuse vanilla's translation key so the tooltip reads "Efficiency".
        setName("digging");
    }

    @Override
    public int getMinEnchantability(int level) {
        return 1 + (level - 1) * 10;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 50;
    }
}
