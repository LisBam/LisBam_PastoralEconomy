package lisbam.pastoraleconomy.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Stable registry names and exact vanilla item scopes for batches 06 and 07. */
public final class ModEnchantments {
    private static final Item[] HOES = new Item[]{
            Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE
    };
    private static final Item[] AXES = new Item[]{
            Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE
    };
    private static final Item[] HOES_SHEARS_AND_AXES = new Item[]{
            Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE,
            Items.SHEARS,
            Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE
    };
    private static final Item[] SWORDS_AND_AXES = new Item[]{
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD,
            Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE
    };
    private static final Item[] BOOTS = new Item[]{
            Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.GOLDEN_BOOTS, Items.DIAMOND_BOOTS
    };
    private static final Item[] LEGGINGS = new Item[]{
            Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS,
            Items.GOLDEN_LEGGINGS, Items.DIAMOND_LEGGINGS
    };
    private static final Item[] HELMETS = new Item[]{
            Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.GOLDEN_HELMET, Items.DIAMOND_HELMET
    };
    private static final Item[] WEAPONS_AND_TOOLS = new Item[]{
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD,
            Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE,
            Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE,
            Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL,
            Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE
    };
    private static final Item[] TOOLS_AND_SHEARS = new Item[]{
            Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE,
            Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE,
            Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL,
            Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE,
            Items.SHEARS
    };
    private static final Item[] RANGE_ITEMS = new Item[]{
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD,
            Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE,
            Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE,
            Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL,
            Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE,
            Items.SHEARS
    };

    public static final Enchantment HARVEST = new EnchantmentPastoral(
            "harvest", Enchantment.Rarity.RARE, 3, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}, HOES_SHEARS_AND_AXES,
            new Enchantment[]{Enchantments.FORTUNE}, AXES
    );
    public static final Enchantment FARMLAND_WALKER = new EnchantmentPastoral(
            "farmland_walker", Enchantment.Rarity.UNCOMMON, 3, new EntityEquipmentSlot[]{EntityEquipmentSlot.FEET}, BOOTS,
            new Enchantment[0]
    );
    public static final Enchantment PASTORAL_FAVOR = new EnchantmentPastoral(
            "pastoral_favor", Enchantment.Rarity.UNCOMMON, 4, new EntityEquipmentSlot[]{EntityEquipmentSlot.HEAD}, HELMETS,
            new Enchantment[0]
    );
    public static final Enchantment FINE_CULTIVATION = new EnchantmentPastoral(
            "fine_cultivation", Enchantment.Rarity.RARE, 4, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}, HOES,
            new Enchantment[0]
    );
    public static final Enchantment FELLING = new EnchantmentPastoral(
            "felling", Enchantment.Rarity.VERY_RARE, 1, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}, AXES,
            new Enchantment[0]
    );
    public static final Enchantment SLAUGHTER = new EnchantmentPastoral(
            "slaughter", Enchantment.Rarity.RARE, 3, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}, SWORDS_AND_AXES,
            new Enchantment[]{Enchantments.SHARPNESS, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS,
                    Enchantments.LOOTING}
    );
    public static final Enchantment FLEETFOOT = new EnchantmentPastoral(
            "fleetfoot", Enchantment.Rarity.UNCOMMON, 4, new EntityEquipmentSlot[]{EntityEquipmentSlot.LEGS}, LEGGINGS,
            new Enchantment[0]
    );
    public static final Enchantment NIGHT_VISION = new EnchantmentPastoral(
            "night_vision", Enchantment.Rarity.RARE, 1, new EntityEquipmentSlot[]{EntityEquipmentSlot.HEAD}, HELMETS,
            new Enchantment[0]
    );
    public static final Enchantment ATTACK_SPEED = new EnchantmentPastoral(
            "attack_speed", Enchantment.Rarity.COMMON, 5, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}, WEAPONS_AND_TOOLS,
            new Enchantment[0]
    );
    public static final Enchantment RANGE = new EnchantmentPastoral(
            "range", Enchantment.Rarity.COMMON, 5, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}, RANGE_ITEMS,
            new Enchantment[0]
    );
    public static final Enchantment DROP_ATTRACTION = new EnchantmentPastoral(
            "drop_attraction", Enchantment.Rarity.VERY_RARE, 1, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}, RANGE_ITEMS,
            new Enchantment[0]
    );
    public static final Enchantment REFORGED = new EnchantmentPastoral(
            "reforged", Enchantment.Rarity.VERY_RARE, 1, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND},
            WEAPONS_AND_TOOLS, new Enchantment[0], false, false, true
    );
    public static final Enchantment BLUNTNESS_CURSE = new EnchantmentPastoral(
            "bluntness_curse", Enchantment.Rarity.VERY_RARE, 1,
            new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND},
            new Item[]{Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD},
            new Enchantment[]{Enchantments.SWEEPING}, true, true, false
    );
    private ModEnchantments() {
    }

    public static Enchantment[] getAll() {
        return new Enchantment[]{
                HARVEST, FARMLAND_WALKER, PASTORAL_FAVOR, FINE_CULTIVATION,
                FELLING, SLAUGHTER, FLEETFOOT, NIGHT_VISION,
                ATTACK_SPEED, RANGE, DROP_ATTRACTION, REFORGED, BLUNTNESS_CURSE
        };
    }

    public static boolean isHoe(ItemStack stack) {
        return hasItem(HOES, stack);
    }

    public static boolean isAxe(ItemStack stack) {
        return hasItem(AXES, stack);
    }

    public static boolean isSwordOrAxe(ItemStack stack) {
        return hasItem(SWORDS_AND_AXES, stack);
    }

    public static boolean isBoots(ItemStack stack) {
        return hasItem(BOOTS, stack);
    }

    public static boolean isLeggings(ItemStack stack) {
        return hasItem(LEGGINGS, stack);
    }

    public static boolean isHelmet(ItemStack stack) {
        return hasItem(HELMETS, stack);
    }

    public static boolean isWeaponOrTool(ItemStack stack) {
        return hasItem(WEAPONS_AND_TOOLS, stack);
    }

    public static boolean isRangeItem(ItemStack stack) {
        return hasItem(RANGE_ITEMS, stack);
    }

    /** The attraction effect applies to the full Range weapon/tool/shears scope, including swords. */
    public static boolean isDropAttractionItem(ItemStack stack) {
        return hasItem(RANGE_ITEMS, stack);
    }

    private static boolean hasItem(Item[] choices, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (Item choice : choices) {
            if (stack.getItem() == choice) {
                return true;
            }
        }
        return false;
    }
}
