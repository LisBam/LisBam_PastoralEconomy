package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Enforces the Bluntness Curse before vanilla computes sweeping eligibility. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class CombatEnchantmentEventHandler {
    private CombatEnchantmentEventHandler() {
    }

    @SubscribeEvent
    public static void disableSwordSweep(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = player.getHeldItemMainhand();
        if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.BLUNTNESS_CURSE, held) <= 0) {
            return;
        }
        // EntityPlayer compares this exact delta immediately after this event.
        // The boundary disables only the horizontal sweep; direct-hit mechanics stay vanilla.
        player.prevDistanceWalkedModified = player.distanceWalkedModified - player.getAIMoveSpeed();
    }
}
