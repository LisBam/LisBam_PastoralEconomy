package lisbam.pastoraleconomy.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Map;

/** Pure 1.12.2 anvil calculation that deliberately starts both inputs at RepairCost 0. */
public final class AnvilFirstUseRules {
    private AnvilFirstUseRules() {
    }

    @Nullable
    public static Result createResult(ItemStack left, ItemStack right, String requestedName) {
        return createResult(left, right, requestedName, false);
    }

    /** Allows Reforged to use the same first-use calculation for the explicit hoe material repair path. */
    @Nullable
    public static Result createResult(ItemStack left, ItemStack right, String requestedName,
                                      boolean additionalRepairMaterial) {
        if (left.isEmpty() || right.isEmpty()) {
            return null;
        }
        ItemStack output = left.copy();
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(output);
        boolean enchantedBook = right.getItem() == Items.ENCHANTED_BOOK
                && !ItemEnchantedBook.getEnchantments(right).hasNoTags();
        int cost = 0;
        int materialCost = 0;

        if (output.isItemStackDamageable()
                && (output.getItem().getIsRepairable(left, right) || additionalRepairMaterial)) {
            int repairAmount = Math.min(output.getItemDamage(), output.getMaxDamage() / 4);
            if (repairAmount <= 0) {
                return null;
            }
            while (repairAmount > 0 && materialCost < right.getCount()) {
                output.setItemDamage(output.getItemDamage() - repairAmount);
                cost++;
                materialCost++;
                repairAmount = Math.min(output.getItemDamage(), output.getMaxDamage() / 4);
            }
        } else {
            if (!enchantedBook && (output.getItem() != right.getItem() || !output.isItemStackDamageable())) {
                return null;
            }
            if (output.isItemStackDamageable() && !enchantedBook) {
                int leftDurability = output.getMaxDamage() - left.getItemDamage();
                int rightDurability = right.getMaxDamage() - right.getItemDamage();
                int combinedDurability = leftDurability + rightDurability + output.getMaxDamage() * 12 / 100;
                int damage = output.getMaxDamage() - combinedDurability;
                if (damage < 0) {
                    damage = 0;
                }
                if (damage < output.getItemDamage()) {
                    output.setItemDamage(damage);
                    cost += 2;
                }
            }

            Map<Enchantment, Integer> incoming = EnchantmentHelper.getEnchantments(right);
            boolean appliedAny = false;
            boolean rejectedAny = false;
            for (Map.Entry<Enchantment, Integer> entry : incoming.entrySet()) {
                Enchantment enchantment = entry.getKey();
                if (enchantment == null) {
                    continue;
                }
                int existingLevel = enchantments.containsKey(enchantment)
                        ? enchantments.get(enchantment).intValue() : 0;
                int incomingLevel = entry.getValue().intValue();
                int combinedLevel = existingLevel == incomingLevel ? incomingLevel + 1
                        : Math.max(incomingLevel, existingLevel);
                boolean compatible = enchantment.canApply(left);
                for (Enchantment present : enchantments.keySet()) {
                    if (present != enchantment && !enchantment.isCompatibleWith(present)) {
                        compatible = false;
                        cost++;
                    }
                }
                if (!compatible) {
                    rejectedAny = true;
                    continue;
                }
                appliedAny = true;
                if (combinedLevel > enchantment.getMaxLevel()) {
                    combinedLevel = enchantment.getMaxLevel();
                }
                enchantments.put(enchantment, Integer.valueOf(combinedLevel));
                int rarityCost = getRarityCost(enchantment);
                if (enchantedBook) {
                    rarityCost = Math.max(1, rarityCost / 2);
                }
                cost += rarityCost * combinedLevel;
                if (left.getCount() > 1) {
                    cost = 40;
                }
            }
            if (rejectedAny && !appliedAny) {
                return null;
            }
        }

        int renameCost = 0;
        if (isBlank(requestedName)) {
            if (left.hasDisplayName()) {
                renameCost = 1;
                cost++;
                output.clearCustomName();
            }
        } else if (!requestedName.equals(left.getDisplayName())) {
            renameCost = 1;
            cost++;
            output.setStackDisplayName(requestedName);
        }

        if (enchantedBook && !output.getItem().isBookEnchantable(output, right)) {
            return null;
        }
        if (cost <= 0) {
            return null;
        }
        if (renameCost == cost && renameCost > 0 && cost >= 40) {
            cost = 39;
        }
        if (cost >= 40) {
            return null;
        }

        // The output begins its next anvil operation exactly like a first use.
        output.setRepairCost(1);
        EnchantmentHelper.setEnchantments(enchantments, output);
        return new Result(output, cost, materialCost);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static int getRarityCost(Enchantment enchantment) {
        switch (enchantment.getRarity()) {
            case COMMON:
                return 1;
            case UNCOMMON:
                return 2;
            case RARE:
                return 4;
            case VERY_RARE:
                return 8;
            default:
                return 1;
        }
    }

    public static final class Result {
        private final ItemStack output;
        private final int cost;
        private final int materialCost;

        private Result(ItemStack output, int cost, int materialCost) {
            this.output = output;
            this.cost = cost;
            this.materialCost = materialCost;
        }

        public ItemStack getOutput() {
            return output;
        }

        public int getCost() {
            return cost;
        }

        public int getMaterialCost() {
            return materialCost;
        }
    }
}
