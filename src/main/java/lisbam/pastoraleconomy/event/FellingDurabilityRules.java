package lisbam.pastoraleconomy.event;

import net.minecraft.item.ItemStack;

/** Fixed felling durability boundary: one point is never spent by the chain. */
public final class FellingDurabilityRules {
    public static final int MIN_REMAINING_DURABILITY = 1;

    private FellingDurabilityRules() {
    }

    public static int getRemainingDurability(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getMaxDamage() <= 0) {
            return 0;
        }
        return Math.max(0, stack.getMaxDamage() - stack.getItemDamage());
    }

    /** Felling is inactive at the final durability point. */
    public static boolean canStartFelling(ItemStack stack) {
        return getRemainingDurability(stack) > MIN_REMAINING_DURABILITY;
    }

    /** Reserve one point for the original log break and one point permanently. */
    public static boolean canHarvestSecondaryLog(ItemStack stack) {
        return getRemainingDurability(stack) > MIN_REMAINING_DURABILITY + 1;
    }
}
