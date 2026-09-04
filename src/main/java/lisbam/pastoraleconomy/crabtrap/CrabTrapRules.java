package lisbam.pastoraleconomy.crabtrap;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.function.IntSupplier;

/**
 * Frozen batch 12/13 crab-trap rules that are independent from a live world.
 * The actual loot result is intentionally delegated to Minecraft 1.12.2's
 * GAMEPLAY_FISHING loot table by {@link lisbam.pastoraleconomy.tile.TileCrabTrap}.
 */
public final class CrabTrapRules {
    public static final int MIN_BASE_WAIT_TICKS = 2000;
    public static final int MAX_BASE_WAIT_TICKS = 12000;
    public static final int LURE_REDUCTION_TICKS = 2000;
    public static final int MAX_FISHING_ENCHANTMENT_LEVEL = 3;

    private CrabTrapRules() {
    }

    public static boolean isFishingRod(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.FISHING_ROD;
    }

    public static boolean isRawMeatBait(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() == Items.BEEF
                || stack.getItem() == Items.PORKCHOP
                || stack.getItem() == Items.CHICKEN
                || stack.getItem() == Items.MUTTON
                || stack.getItem() == Items.RABBIT;
    }

    public static int clampFishingEnchantmentLevel(int level) {
        return Math.max(0, Math.min(MAX_FISHING_ENCHANTMENT_LEVEL, level));
    }

    /**
     * Draw a complete batch-12/13 wait time. Non-positive Lure-adjusted
     * draws are discarded and the supplier is asked for another base time.
     */
    public static int rollRoundWaitTicks(int lureLevel, boolean hasBait, IntSupplier baseWaitSupplier) {
        int clampedLure = clampFishingEnchantmentLevel(lureLevel);
        int adjusted;
        do {
            int base = baseWaitSupplier.getAsInt();
            if (base < MIN_BASE_WAIT_TICKS || base > MAX_BASE_WAIT_TICKS) {
                throw new IllegalArgumentException("Crab trap base wait must be between 2000 and 12000 ticks.");
            }
            adjusted = base - clampedLure * LURE_REDUCTION_TICKS;
        } while (adjusted <= 0);

        if (!hasBait) {
            return adjusted;
        }
        return Math.max(1, (adjusted + 1) / 2);
    }

    /** Root fishing-table weights after Minecraft 1.12.2 quality * luck. */
    public static int getFishCategoryWeight(int luckLevel) {
        return 85 - clampFishingEnchantmentLevel(luckLevel);
    }

    public static int getJunkCategoryWeight(int luckLevel) {
        return 10 - 2 * clampFishingEnchantmentLevel(luckLevel);
    }

    public static int getTreasureCategoryWeight(int luckLevel) {
        return 5 + 2 * clampFishingEnchantmentLevel(luckLevel);
    }
}
