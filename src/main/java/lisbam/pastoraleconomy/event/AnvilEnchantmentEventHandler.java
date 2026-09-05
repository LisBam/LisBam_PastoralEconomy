package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.enchantment.AnvilFirstUseRules;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
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
        AnvilFirstUseRules.Result result = AnvilFirstUseRules.createResult(left, event.getRight(), event.getName());
        if (result == null) {
            return;
        }
        event.setOutput(result.getOutput());
        event.setCost(result.getCost());
        event.setMaterialCost(result.getMaterialCost());
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
}
