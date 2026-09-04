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
    private static final Item[] SWORDS_AND_AXES = new Item[]{
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD,
            Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE
    };
    private static final Item[] BOOTS = new Item[]{
            Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.GOLDEN_BOOTS, Items.DIAMOND_BOOTS
    };
    private static final Item[] HELMETS = new Item[]{
            Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.GOLDEN_HELMET, Items.DIAMOND_HELMET
    };

    public static final Enchantment HARVEST = new EnchantmentPastoral(
            "harvest", 3, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}, HOES,
            new Enchantment[]{Enchantments.FORTUNE}
    );
    public static final Enchantment FARMLAND_WALKER = new EnchantmentPastoral(
            "farmland_walker", 3, new EntityEquipmentSlot[]{EntityEquipmentSlot.FEET}, BOOTS,
            new Enchantment[0]
    );
    public static final Enchantment PASTORAL_FAVOR = new EnchantmentPastoral(
            "pastoral_favor", 4, new EntityEquipmentSlot[]{EntityEquipmentSlot.HEAD}, HELMETS,
            new Enchantment[0]
    );
    public static final Enchantment FINE_CULTIVATION = new EnchantmentPastoral(
            "fine_cultivation", 4, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}, HOES,
            new Enchantment[0]
    );
    public static final Enchantment FELLING = new EnchantmentPastoral(
            "felling", 1, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}, AXES,
            new Enchantment[0]
    );
    public static final Enchantment SLAUGHTER = new EnchantmentPastoral(
            "slaughter", 3, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}, SWORDS_AND_AXES,
            new Enchantment[]{Enchantments.SHARPNESS, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS,
                    Enchantments.LOOTING}
    );
    public static final Enchantment FLEETFOOT = new EnchantmentPastoral(
            "fleetfoot", 4, new EntityEquipmentSlot[]{EntityEquipmentSlot.FEET}, BOOTS,
            new Enchantment[0]
    );
    public static final Enchantment NIGHT_VISION = new EnchantmentPastoral(
            "night_vision", 1, new EntityEquipmentSlot[]{EntityEquipmentSlot.HEAD}, HELMETS,
            new Enchantment[0]
    );

    private ModEnchantments() {
    }

    public static Enchantment[] getAll() {
        return new Enchantment[]{
                HARVEST, FARMLAND_WALKER, PASTORAL_FAVOR, FINE_CULTIVATION,
                FELLING, SLAUGHTER, FLEETFOOT, NIGHT_VISION
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

    public static boolean isHelmet(ItemStack stack) {
        return hasItem(HELMETS, stack);
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
