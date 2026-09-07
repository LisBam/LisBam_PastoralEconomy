package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.equipment.FeatherWingsRules;
import lisbam.pastoraleconomy.enchantment.AnvilFirstUseRules;
import lisbam.pastoraleconomy.enchantment.HoeAnvilRepairRules;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import lisbam.pastoraleconomy.item.ItemFeatherWings;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Generates a transient first-use anvil result without mutating either input item's NBT. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class AnvilEnchantmentEventHandler {
    private AnvilEnchantmentEventHandler() {
    }

    @SubscribeEvent
    public static void removePriorWorkPenalty(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        if (left.isEmpty() || EnchantmentHelper.getEnchantmentLevel(ModEnchantments.REFORGED, left) <= 0) {
            return;
        }
        AnvilFirstUseRules.Result result = AnvilFirstUseRules.createResult(left, event.getRight(), event.getName(),
                HoeAnvilRepairRules.isHoeRepairMaterial(left, event.getRight()));
        if (result == null) {
            return;
        }
        event.setOutput(result.getOutput());
        event.setCost(result.getCost());
        event.setMaterialCost(result.getMaterialCost());
    }

    /**
     * Minecraft/Forge 1.12.2's ItemHoe inherits Item#getIsRepairable, which
     * always returns false. Restore the ordinary material-repair calculation
     * only for the five vanilla hoes and leave every other anvil item alone.
     */
    @SubscribeEvent
    public static void repairVanillaHoesWithTheirMaterials(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        if (left.isEmpty() || EnchantmentHelper.getEnchantmentLevel(ModEnchantments.REFORGED, left) > 0) {
            return;
        }
        HoeAnvilRepairRules.Result result = HoeAnvilRepairRules.createVanillaRepairResult(
                left, event.getRight(), event.getName());
        if (result == null) {
            return;
        }
        event.setOutput(result.getOutput());
        event.setCost(result.getCost());
        event.setMaterialCost(result.getMaterialCost());
    }

    /** Feathers repair exactly one percent (five of 500 durability) each. */
    @SubscribeEvent
    public static void repairFeatherWingsWithFeathers(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!ItemFeatherWings.isFeatherWings(left) || right.isEmpty() || right.getItem() != Items.FEATHER) {
            return;
        }
        int feathers = FeatherWingsRules.getFeathersRequired(left.getItemDamage(), right.getCount());
        int repaired = FeatherWingsRules.getRepairedDamage(left.getItemDamage(), feathers);
        if (feathers <= 0 || repaired <= 0) {
            return;
        }

        ItemStack output = left.copy();
        output.setItemDamage(left.getItemDamage() - repaired);
        boolean renamed = applyRequestedName(output, event.getName());
        output.setRepairCost(FeatherWingsRules.getNextRepairCost(left.getRepairCost()));
        event.setOutput(output);
        event.setMaterialCost(feathers);
        event.setCost(FeatherWingsRules.getAnvilExperienceCost(left.getRepairCost(), renamed));
    }

    /**
     * Forge only exposes AnvilUpdateEvent when the right input is non-empty.
     * A plain rename therefore receives the same transient adjustment after
     * vanilla has prepared its output, while neither input stack is changed.
     */
    @SubscribeEvent
    public static void removeRenamePriorWorkPenalty(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote
                || !(event.player.openContainer instanceof ContainerRepair)) {
            return;
        }
        ContainerRepair anvil = (ContainerRepair) event.player.openContainer;
        ItemStack left = anvil.getSlot(0).getStack();
        ItemStack right = anvil.getSlot(1).getStack();
        ItemStack output = anvil.getSlot(2).getStack();
        if (left.isEmpty() || !right.isEmpty() || output.isEmpty()
                || EnchantmentHelper.getEnchantmentLevel(ModEnchantments.REFORGED, left) <= 0) {
            return;
        }
        int priorWorkCost = left.getRepairCost();
        if (priorWorkCost <= 0) {
            return;
        }
        int firstUseCost = anvil.maximumCost - priorWorkCost;
        if (firstUseCost <= 0) {
            return;
        }
        anvil.maximumCost = Math.max(1, firstUseCost);
        output.setRepairCost(0);
        anvil.getSlot(2).putStack(output);
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
}
