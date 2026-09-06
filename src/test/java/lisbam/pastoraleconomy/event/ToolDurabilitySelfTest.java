package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.agriculture.AgricultureRules;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/** Regression checks for felling's reserve and the hoe crop-wear boundary. */
public final class ToolDurabilitySelfTest {
    private ToolDurabilitySelfTest() {
    }

    public static void main(String[] args) {
        Bootstrap.register();
        verifyFellingReserve();
        verifyHoeCropWearScope();
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
        require(AgricultureRules.shouldConsumeHoeCropDurability(Blocks.WHEAT.getDefaultState(), 0.0F)
                        && AgricultureRules.shouldConsumeHoeCropDurability(Blocks.NETHER_WART.getDefaultState(), 0.0F),
                "zero-hardness planted crops must add the hoe's missing durability loss");
        require(!AgricultureRules.shouldConsumeHoeCropDurability(Blocks.MELON_BLOCK.getDefaultState(), 1.0F),
                "nonzero-hardness crop blocks already use ItemHoe's vanilla durability loss");
        require(!AgricultureRules.shouldConsumeHoeCropDurability(Blocks.GRASS.getDefaultState(), 0.0F),
                "non-crop blocks must not consume extra hoe durability");
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
