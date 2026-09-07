package lisbam.pastoraleconomy.enchantment;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;

/** Restores the ordinary 1.12.2 material-repair path omitted by vanilla ItemHoe. */
public final class HoeAnvilRepairRules {
    private HoeAnvilRepairRules() {
    }

    public static boolean isHoeRepairMaterial(ItemStack hoe, ItemStack material) {
        Item.ToolMaterial toolMaterial = getToolMaterial(hoe);
        if (toolMaterial == null || material.isEmpty()) {
            return false;
        }
        ItemStack expected = toolMaterial.getRepairItemStack();
        return !expected.isEmpty() && OreDictionary.itemMatches(expected, material, false);
    }

    @Nullable
    public static Result createVanillaRepairResult(ItemStack left, ItemStack right, String requestedName) {
        if (!isHoeRepairMaterial(left, right) || !left.isItemStackDamageable()) {
            return null;
        }
        ItemStack output = left.copy();
        int repairAmount = Math.min(output.getItemDamage(), output.getMaxDamage() / 4);
        if (repairAmount <= 0) {
            return null;
        }
        int materialCost = 0;
        while (repairAmount > 0 && materialCost < right.getCount()) {
            output.setItemDamage(output.getItemDamage() - repairAmount);
            materialCost++;
            repairAmount = Math.min(output.getItemDamage(), output.getMaxDamage() / 4);
        }
        if (materialCost <= 0) {
            return null;
        }

        int operationCost = materialCost;
        boolean renamed = applyRequestedName(output, requestedName);
        if (renamed) {
            operationCost++;
        }
        int priorWorkCost = Math.max(left.getRepairCost(), right.getRepairCost());
        output.setRepairCost(nextRepairCost(priorWorkCost));
        return new Result(output, addCosts(priorWorkCost, operationCost), materialCost);
    }

    @Nullable
    private static Item.ToolMaterial getToolMaterial(ItemStack hoe) {
        if (hoe.isEmpty()) {
            return null;
        }
        if (hoe.getItem() == Items.WOODEN_HOE) {
            return Item.ToolMaterial.WOOD;
        }
        if (hoe.getItem() == Items.STONE_HOE) {
            return Item.ToolMaterial.STONE;
        }
        if (hoe.getItem() == Items.IRON_HOE) {
            return Item.ToolMaterial.IRON;
        }
        if (hoe.getItem() == Items.GOLDEN_HOE) {
            return Item.ToolMaterial.GOLD;
        }
        if (hoe.getItem() == Items.DIAMOND_HOE) {
            return Item.ToolMaterial.DIAMOND;
        }
        return null;
    }

    private static boolean applyRequestedName(ItemStack output, String requestedName) {
        if (requestedName == null || requestedName.trim().isEmpty()) {
            if (output.hasDisplayName()) {
                output.clearCustomName();
                return true;
            }
            return false;
        }
        if (!requestedName.equals(output.getDisplayName())) {
            output.setStackDisplayName(requestedName);
            return true;
        }
        return false;
    }

    private static int nextRepairCost(int currentRepairCost) {
        return currentRepairCost > (Integer.MAX_VALUE - 1) / 2
                ? Integer.MAX_VALUE : currentRepairCost * 2 + 1;
    }

    private static int addCosts(int first, int second) {
        return first > Integer.MAX_VALUE - second ? Integer.MAX_VALUE : first + second;
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
