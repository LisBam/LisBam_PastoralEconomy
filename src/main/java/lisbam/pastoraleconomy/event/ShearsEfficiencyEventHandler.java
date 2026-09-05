package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemShears;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Applies vanilla Efficiency's speed formula to the shears-only compatibility enchantment. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class ShearsEfficiencyEventHandler {
    private ShearsEfficiencyEventHandler() {
    }

    @SubscribeEvent
    public static void applyEfficiency(PlayerEvent.BreakSpeed event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = player.getHeldItemMainhand();
        if (!(held.getItem() instanceof ItemShears) || event.getState().getBlockHardness(player.world, event.getPos()) <= 0.0F) {
            return;
        }
        int level = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SHEARS_EFFICIENCY, held);
        if (level > 0) {
            event.setNewSpeed(event.getNewSpeed() + level * level + 1.0F);
        }
    }
}
