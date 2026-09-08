package lisbam.pastoraleconomy.equipment;

import lisbam.pastoraleconomy.item.ItemFeatherWings;
import lisbam.pastoraleconomy.item.ModItems;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/** Deterministic item, repair, enchantment and hunger-balance checks for Feather Wings. */
public final class FeatherWingsSelfTest {
    private FeatherWingsSelfTest() {
    }

    public static void main(String[] args) {
        Bootstrap.register();
        ItemStack wings = new ItemStack(ModItems.FEATHER_WINGS);
        require(wings.getMaxDamage() == 500 && wings.getMaxStackSize() == 1,
                "Feather Wings must have 500 durability and stack to one");
        require(ItemFeatherWings.isFeatherWings(wings) && ShoulderEquipmentService.isValidShoulderStack(wings),
                "Feather Wings must be valid shoulder equipment");
        require(ModItems.FEATHER_WINGS.getIsRepairable(wings, new ItemStack(Items.FEATHER)),
                "feathers must be the repair material");
        require(!ModItems.FEATHER_WINGS.getIsRepairable(wings, new ItemStack(Items.PAPER)),
                "only feathers may repair Feather Wings");
        require(ModItems.FEATHER_WINGS.canApplyAtEnchantingTable(wings, Enchantments.UNBREAKING),
                "Unbreaking must be the only table enchantment");
        require(ModItems.FEATHER_WINGS.canApplyAtEnchantingTable(wings, Enchantments.MENDING)
                        && ModItems.FEATHER_WINGS.canApplyAtEnchantingTable(wings, Enchantments.BINDING_CURSE)
                        && ModItems.FEATHER_WINGS.canApplyAtEnchantingTable(wings, Enchantments.VANISHING_CURSE),
                "the Item hook must admit Mending and curses for enchanted-book anvils");
        require(Enchantments.MENDING.canApply(wings) && Enchantments.BINDING_CURSE.canApply(wings)
                        && Enchantments.VANISHING_CURSE.canApply(wings),
                "Mending and both curses must apply through vanilla books/anvils");
        require(FeatherWingsRules.FLIGHT_TICKS_PER_DURABILITY == 20,
                "one durability point must be consumed every second");
        require(FeatherWingsRules.getFeathersRequired(500, 100) == 100
                        && FeatherWingsRules.getRepairedDamage(500, 100) == 500,
                "100 feathers must fully repair 500 damage");
        require(FeatherWingsRules.getFeathersRequired(6, 10) == 2
                        && FeatherWingsRules.getRepairedDamage(6, 2) == 6,
                "partial damage must consume only enough whole feathers");
        require(Math.abs(FeatherWingsRules.getFlightExhaustion(100) - 0.02F) < 0.000001F
                        && FeatherWingsRules.getFlightExhaustion(1001) == 0.0F,
                "flight exhaustion must be twice walking exhaustion and reject teleports");
        System.out.println("featherWingsSelfTest PASS");
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
