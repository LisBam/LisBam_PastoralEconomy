package lisbam.pastoraleconomy.enchantment;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Shared legacy-Forge definition for this batch's item-restricted enchantments.
 *
 * <p>The frozen design specifies rarity and levels but not enchanting-table
 * thresholds. The 15 + 9 * (level - 1) / +15 interval is deliberately close to
 * vanilla rare utility enchantments and is kept in one place so it is not
 * silently different between the eight entries.</p>
 */
public class EnchantmentPastoral extends Enchantment {
    private final int maxLevel;
    private final Set<Item> allowedItems;
    private final Set<Enchantment> incompatibleEnchantments;

    protected EnchantmentPastoral(String path, int maxLevel, EntityEquipmentSlot[] slots,
                                  Item[] allowedItems, Enchantment[] incompatibleEnchantments) {
        super(Rarity.RARE, EnumEnchantmentType.ALL, slots);
        this.maxLevel = maxLevel;
        this.allowedItems = new HashSet<Item>(Arrays.asList(allowedItems));
        this.incompatibleEnchantments = new HashSet<Enchantment>(Arrays.asList(incompatibleEnchantments));
        setRegistryName(new ResourceLocation(LisBamPastoralEconomy.MODID, path));
        setName(LisBamPastoralEconomy.MODID + "." + path);
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public int getMinEnchantability(int level) {
        return 15 + (level - 1) * 9;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 15;
    }

    @Override
    public boolean canApply(ItemStack stack) {
        return !stack.isEmpty() && allowedItems.contains(stack.getItem());
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return canApply(stack);
    }

    @Override
    protected boolean canApplyTogether(Enchantment other) {
        return !incompatibleEnchantments.contains(other) && super.canApplyTogether(other);
    }
}
