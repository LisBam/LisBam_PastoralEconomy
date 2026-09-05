package lisbam.pastoraleconomy.enchantment;

import lisbam.pastoraleconomy.agriculture.AgricultureRules;
import lisbam.pastoraleconomy.event.ToolEnchantmentEventHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.Enchantment.Rarity;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemEnchantedBook;

import java.util.Random;

/** Standalone deterministic checks for all registered pastoral enchantments. */
public final class EnchantmentSelfTest {
    private EnchantmentSelfTest() {
    }

    public static void main(String[] args) {
        Bootstrap.register();
        verifyDefinitions();
        verifyReforgedAnvilRule();
        verifyHarvestFormulas();
        verifyProbabilityThresholds();
    }

    private static void verifyReforgedAnvilRule() {
        ItemStack left = new ItemStack(Items.DIAMOND_SWORD);
        left.setRepairCost(31);
        ItemStack right = new ItemStack(Items.ENCHANTED_BOOK);
        right.setRepairCost(15);
        ItemEnchantedBook.addEnchantment(right, new EnchantmentData(Enchantments.UNBREAKING, 1));

        AnvilFirstUseRules.Result result = AnvilFirstUseRules.createResult(left, right, null);
        require(result != null, "Reforged must produce an ordinary first-use result despite prior-work costs");
        require(result.getCost() == 1, "Reforged must charge the first-use book cost");
        require(result.getOutput().getRepairCost() == 1, "Reforged output must restart at first-use repair cost");
        require(left.getRepairCost() == 31 && right.getRepairCost() == 15,
                "Reforged must not mutate either anvil input's persistent NBT");
    }

    private static void verifyDefinitions() {
        Enchantment[] all = ModEnchantments.getAll();
        require(all.length == 13, "exactly thirteen formal enchantments must be registered");
        for (Enchantment enchantment : all) {
            require(enchantment.isAllowedOnBooks(), "all batch enchantments must allow enchanted books");
        }
        require(ModEnchantments.ATTACK_SPEED.getRarity() == Rarity.RARE, "Attack Speed rarity");
        require(ModEnchantments.RANGE.getRarity() == Rarity.RARE, "Range rarity");
        require(ModEnchantments.REFORGED.getRarity() == Rarity.RARE, "Reforged Mending rarity");
        require(ModEnchantments.BLUNTNESS_CURSE.getRarity() == Rarity.VERY_RARE
                        && ModEnchantments.BLUNTNESS_CURSE.isTreasureEnchantment()
                        && ModEnchantments.BLUNTNESS_CURSE.isCurse(),
                "Bluntness Curse must be a treasure curse");
        require(ModEnchantments.HARVEST.getMaxLevel() == 3, "Harvest maximum level");
        require(ModEnchantments.FARMLAND_WALKER.getMaxLevel() == 3, "Farmland Walker maximum level");
        require(ModEnchantments.PASTORAL_FAVOR.getMaxLevel() == 4, "Pastoral Favor maximum level");
        require(ModEnchantments.FINE_CULTIVATION.getMaxLevel() == 4, "Fine Cultivation maximum level");
        require(ModEnchantments.FELLING.getMaxLevel() == 1, "Felling maximum level");
        require(ModEnchantments.SLAUGHTER.getMaxLevel() == 3, "Slaughter maximum level");
        require(ModEnchantments.FLEETFOOT.getMaxLevel() == 4, "Fleetfoot maximum level");
        require(ModEnchantments.NIGHT_VISION.getMaxLevel() == 1, "Night Vision maximum level");
        require(ModEnchantments.ATTACK_SPEED.getMaxLevel() == 5, "Attack Speed maximum level");
        require(ModEnchantments.RANGE.getMaxLevel() == 5, "Range maximum level");
        require(ModEnchantments.REFORGED.getMaxLevel() == 1, "Reforged maximum level");
        require(ModEnchantments.BLUNTNESS_CURSE.getMaxLevel() == 1, "Bluntness Curse maximum level");
        require(ModEnchantments.SHEARS_EFFICIENCY.getMaxLevel() == 5
                        && ModEnchantments.SHEARS_EFFICIENCY.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS))
                        && !ModEnchantments.SHEARS_EFFICIENCY.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_HOE)),
                "shears Efficiency compatibility");
        require(ModEnchantments.HARVEST.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_HOE)),
                "Harvest must apply to a vanilla hoe");
        require(!ModEnchantments.HARVEST.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_AXE)),
                "Harvest must reject a non-hoe");
        require(ModEnchantments.HARVEST.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS)),
                "Harvest must apply to shears");
        require(Enchantments.MENDING.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS))
                        && Enchantments.UNBREAKING.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS))
                        && Enchantments.VANISHING_CURSE.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS)),
                "vanilla BREAKABLE enchantments must apply to shears");
        require(ModEnchantments.SLAUGHTER.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD)),
                "Slaughter must apply to a sword");
        require(ModEnchantments.SLAUGHTER.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_AXE)),
                "Slaughter must apply to an axe");
        require(!ModEnchantments.SLAUGHTER.isCompatibleWith(Enchantments.SHARPNESS),
                "Slaughter must reject Sharpness");
        require(!ModEnchantments.SLAUGHTER.isCompatibleWith(Enchantments.SMITE),
                "Slaughter must reject Smite");
        require(!ModEnchantments.SLAUGHTER.isCompatibleWith(Enchantments.BANE_OF_ARTHROPODS),
                "Slaughter must reject Bane of Arthropods");
        require(!ModEnchantments.SLAUGHTER.isCompatibleWith(Enchantments.LOOTING),
                "Slaughter must reject Looting");
        require(!ModEnchantments.HARVEST.isCompatibleWith(Enchantments.FORTUNE),
                "Harvest must reject Fortune");
        require(ModEnchantments.ATTACK_SPEED.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD)),
                "Attack Speed must apply to weapons");
        require(ModEnchantments.FLEETFOOT.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_LEGGINGS))
                        && !ModEnchantments.FLEETFOOT.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_BOOTS)),
                "Fleetfoot must apply to leggings, not boots");
        require(ModEnchantments.ATTACK_SPEED.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_PICKAXE)),
                "Attack Speed must apply to tools");
        require(ModEnchantments.RANGE.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS)),
                "Range must apply to shears");
        require(!ModEnchantments.ATTACK_SPEED.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS)),
                "Attack Speed must not extend beyond the stated shears enchantment list");
        require(ModEnchantments.REFORGED.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_AXE)),
                "Reforged must apply to tools");
        require(!ModEnchantments.REFORGED.canApplyAtEnchantingTable(new ItemStack(Items.SHEARS)),
                "Reforged must not extend beyond the stated shears enchantment list");
        require(ModEnchantments.BLUNTNESS_CURSE.canApply(new ItemStack(Items.DIAMOND_SWORD)),
                "Bluntness Curse item scope must be swords");
        require(!ModEnchantments.BLUNTNESS_CURSE.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD)),
                "Bluntness Curse must stay out of enchanting-table rolls");
        require(!ModEnchantments.BLUNTNESS_CURSE.isCompatibleWith(Enchantments.SWEEPING),
                "Bluntness Curse must reject Sweeping Edge");
        require(ToolEnchantmentEventHandler.getRangeBonus(1) == 1.5D
                        && ToolEnchantmentEventHandler.getRangeBonus(5) == 7.5D,
                "Range must add exactly 1.5 blocks per enchantment level");
    }

    private static void verifyHarvestFormulas() {
        ItemStack carrots = AgricultureRules.createHarvestBonus(AgricultureRules.Crop.CARROT, 1,
                new FixedRandom(0, 4));
        require(carrots.getItem() == Items.CARROT && carrots.getCount() == 1,
                "carrot Harvest must use two independent 4/7 trials per level");

        ItemStack wheat = AgricultureRules.createHarvestBonus(AgricultureRules.Crop.WHEAT, 2,
                new FixedRandom(0, 1, 0, 1));
        require(wheat.getItem() == Items.WHEAT && wheat.getCount() == 2,
                "wheat Harvest must use two 1/2 trials per level");

        ItemStack wart = AgricultureRules.createHarvestBonus(AgricultureRules.Crop.NETHER_WART, 3,
                new FixedRandom(6));
        require(wart.getItem() == Items.NETHER_WART && wart.getCount() == 6,
                "nether wart Harvest upper bound must be 2L");

        ItemStack melon = AgricultureRules.createHarvestBonus(AgricultureRules.Crop.MELON, 3,
                new FixedRandom(3));
        require(melon.getItem() == Items.MELON && melon.getCount() == 6,
                "melon Harvest must be UniformInt(L, 2L)");

        ItemStack cocoa = AgricultureRules.createHarvestBonus(AgricultureRules.Crop.COCOA, 3,
                new FixedRandom());
        require(cocoa.getItem() == Items.DYE && cocoa.getCount() == 3 && cocoa.getMetadata() == 3,
                "cocoa Harvest must add fixed cocoa-bean dye metadata 3");

        require(AgricultureRules.createHarvestBonus(AgricultureRules.Crop.PUMPKIN, 3,
                new FixedRandom()).isEmpty(), "pumpkins must receive no Harvest bonus");
        require(AgricultureRules.rollShearingHarvestBonus(2, new FixedRandom(0, 4, 1, 6)) == 2,
                "shearing Harvest must use two 4/7 trials per level");
    }

    private static void verifyProbabilityThresholds() {
        require(AgricultureRules.rollFineCultivation(1, new FixedRandom(24)), "Fine Cultivation I succeeds below 25");
        require(!AgricultureRules.rollFineCultivation(1, new FixedRandom(25)), "Fine Cultivation I fails at 25");
        require(AgricultureRules.rollFineCultivation(4, new FixedRandom(99)), "Fine Cultivation IV is 100 percent");
        require(AgricultureRules.rollPastoralFavor(4, new FixedRandom(39)), "Pastoral Favor IV succeeds below 40");
        require(!AgricultureRules.rollPastoralFavor(4, new FixedRandom(40)), "Pastoral Favor IV fails at 40");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class FixedRandom extends Random {
        private final int[] values;
        private int index;

        private FixedRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            if (index >= values.length) {
                throw new AssertionError("test random exhausted for bound " + bound);
            }
            int value = values[index++];
            if (value < 0 || value >= bound) {
                throw new AssertionError("test random value " + value + " outside bound " + bound);
            }
            return value;
        }
    }
}
