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
    private final Set<Item> enchantingTableExcludedItems;
    private final boolean treasure;
    private final boolean curse;
    private final boolean availableAtEnchantingTable;

    protected EnchantmentPastoral(String path, int maxLevel, EntityEquipmentSlot[] slots,
                                  Item[] allowedItems, Enchantment[] incompatibleEnchantments) {
        this(path, Rarity.RARE, maxLevel, slots, allowedItems, incompatibleEnchantments,
                false, false, true, new Item[0]);
    }

    /** Allows books/anvils without adding a specific allowed item to table rolls. */
    protected EnchantmentPastoral(String path, int maxLevel, EntityEquipmentSlot[] slots,
                                  Item[] allowedItems, Enchantment[] incompatibleEnchantments,
                                  Item[] enchantingTableExcludedItems) {
        this(path, Rarity.RARE, maxLevel, slots, allowedItems, incompatibleEnchantments,
                false, false, true, enchantingTableExcludedItems);
    }

    protected EnchantmentPastoral(String path, Rarity rarity, int maxLevel, EntityEquipmentSlot[] slots,
                                  Item[] allowedItems, Enchantment[] incompatibleEnchantments,
                                  boolean treasure, boolean curse, boolean availableAtEnchantingTable) {
        this(path, rarity, maxLevel, slots, allowedItems, incompatibleEnchantments,
                treasure, curse, availableAtEnchantingTable, new Item[0]);
    }

    protected EnchantmentPastoral(String path, Rarity rarity, int maxLevel, EntityEquipmentSlot[] slots,
                                  Item[] allowedItems, Enchantment[] incompatibleEnchantments,
                                  boolean treasure, boolean curse, boolean availableAtEnchantingTable,
                                  Item[] enchantingTableExcludedItems) {
        super(rarity, EnumEnchantmentType.ALL, slots);
        this.maxLevel = maxLevel;
        this.allowedItems = new HashSet<Item>(Arrays.asList(allowedItems));
        this.incompatibleEnchantments = new HashSet<Enchantment>(Arrays.asList(incompatibleEnchantments));
        this.enchantingTableExcludedItems = new HashSet<Item>(Arrays.asList(enchantingTableExcludedItems));
        this.treasure = treasure;
        this.curse = curse;
        this.availableAtEnchantingTable = availableAtEnchantingTable;
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
        return availableAtEnchantingTable && canApply(stack)
                && !enchantingTableExcludedItems.contains(stack.getItem());
    }

    @Override
    public boolean isTreasureEnchantment() {
        return treasure;
    }

    @Override
    public boolean isCurse() {
        return curse;
    }

    @Override
    protected boolean canApplyTogether(Enchantment other) {
        return !incompatibleEnchantments.contains(other) && super.canApplyTogether(other);
    }
}
