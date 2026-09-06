package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.agriculture.AgricultureRules;
import net.minecraft.block.BlockBeetroot;
import net.minecraft.block.BlockCrops;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.Random;

/** Regression checks for felling's reserve and the hoe crop-wear boundary. */
public final class ToolDurabilitySelfTest {
    private ToolDurabilitySelfTest() {
    }

    public static void main(String[] args) {
        Bootstrap.register();
        verifyFellingReserve();
        verifyHoeCropWearScope();
        verifyFineCultivationReplantAge();
        System.out.println("toolDurabilitySelfTest PASS");
    }

    private static void verifyFellingReserve() {
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        int maxDamage = axe.getMaxDamage();
        axe.setItemDamage(maxDamage - 1);
        require(FellingDurabilityRules.getRemainingDurability(axe) == 1
                        && !FellingDurabilityRules.canStartFelling(axe),
                "Felling must be inactive at the final durability point");

        axe.setItemDamage(maxDamage - 2);
        require(FellingDurabilityRules.canStartFelling(axe)
                        && !FellingDurabilityRules.canHarvestSecondaryLog(axe),
                "two remaining points must reserve the root log and final point");

        axe.setItemDamage(maxDamage - 3);
        require(FellingDurabilityRules.canHarvestSecondaryLog(axe),
                "a secondary log requires one point for itself, the root and the reserve");
    }

    private static void verifyHoeCropWearScope() {
        ItemStack hoe = new ItemStack(Items.DIAMOND_HOE);
        require(HoeCropDurabilityEventHandler.shouldQueueHoeWear(
                        hoe, Blocks.WHEAT.getDefaultState(), 0.0F, false)
                        && HoeCropDurabilityEventHandler.shouldQueueHoeWear(
                        hoe, Blocks.NETHER_WART.getDefaultState(), 0.0F, false),
                "zero-hardness planted crops must add the hoe's missing durability loss");
        require(!HoeCropDurabilityEventHandler.shouldQueueHoeWear(
                        hoe, Blocks.WHEAT.getDefaultState(), 0.0F, true),
                "creative harvests must not queue hoe wear");
        require(!HoeCropDurabilityEventHandler.shouldQueueHoeWear(
                        hoe, Blocks.MELON_BLOCK.getDefaultState(), 1.0F, false),
                "nonzero-hardness crop blocks already use ItemHoe's vanilla durability loss");
        require(!HoeCropDurabilityEventHandler.shouldQueueHoeWear(
                        hoe, Blocks.GRASS.getDefaultState(), 0.0F, false),
                "non-crop blocks must not consume extra hoe durability");
        require(!HoeCropDurabilityEventHandler.shouldQueueHoeWear(
                        new ItemStack(Items.DIAMOND_AXE), Blocks.WHEAT.getDefaultState(), 0.0F, false),
                "non-hoe tools must not enter the deferred wear pipeline");

        boolean broke = hoe.attemptDamageItem(1, new Random(0L), null);
        require(!broke && hoe.getItemDamage() == 1,
                "a confirmed unenchanted hoe harvest must consume exactly one durability point");
    }

    private static void verifyFineCultivationReplantAge() {
        require(AgricultureRules.getFineCultivationReplantState(AgricultureRules.Crop.WHEAT)
                        .getValue(BlockCrops.AGE).intValue() == 0,
                "Fine Cultivation wheat must replant at age zero");
        require(AgricultureRules.getFineCultivationReplantState(AgricultureRules.Crop.CARROT)
                        .getValue(BlockCrops.AGE).intValue() == 0,
                "Fine Cultivation carrots must replant at age zero");
        require(AgricultureRules.getFineCultivationReplantState(AgricultureRules.Crop.POTATO)
                        .getValue(BlockCrops.AGE).intValue() == 0,
                "Fine Cultivation potatoes must replant at age zero");
        require(AgricultureRules.getFineCultivationReplantState(AgricultureRules.Crop.BEETROOT)
                        .getValue(BlockBeetroot.BEETROOT_AGE).intValue() == 0,
                "Fine Cultivation beetroot must replant at age zero");
        require(AgricultureRules.getFineCultivationReplantState(AgricultureRules.Crop.NETHER_WART) == null,
                "unsupported crops must not produce a Fine Cultivation replant state");
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
