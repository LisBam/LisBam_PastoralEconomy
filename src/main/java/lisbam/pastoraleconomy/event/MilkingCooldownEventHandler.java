package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.config.ModSettings;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Enforces a persistent five-minute cooldown for milking each adult cow. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class MilkingCooldownEventHandler {
    private static final String LAST_MILKED_TICK_KEY = "lisbam_pastoral_economy_last_milked_tick";

    private MilkingCooldownEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void handleMilking(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || !(event.getTarget() instanceof EntityCow)) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || held.getItem() != Items.BUCKET) {
            return;
        }
        EntityCow cow = (EntityCow) event.getTarget();
        if (cow.isChild() || event.getEntityPlayer().capabilities.isCreativeMode) {
            return;
        }

        // The logical server is authoritative.  Leave the vanilla path alone
        // when the option is disabled so existing worlds retain vanilla timing.
        if (event.getWorld().isRemote || ModSettings.isDisableMilkingCooldown()) {
            return;
        }

        long now = event.getWorld().getTotalWorldTime();
        long last = cow.getEntityData().hasKey(LAST_MILKED_TICK_KEY)
                ? cow.getEntityData().getLong(LAST_MILKED_TICK_KEY) : -1L;
        if (MilkCooldownRules.isOnCooldown(last, now)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.FAIL);
            return;
        }

        // Reproduce EntityCow.processInteract's successful bucket branch and
        // cancel the original call to prevent a second interaction.
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.getEntityPlayer().playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
        held.shrink(1);
        if (held.isEmpty()) {
            event.getEntityPlayer().setHeldItem(event.getHand(), new ItemStack(Items.MILK_BUCKET));
        } else if (!event.getEntityPlayer().inventory.addItemStackToInventory(new ItemStack(Items.MILK_BUCKET))) {
            event.getEntityPlayer().dropItem(new ItemStack(Items.MILK_BUCKET), false);
        }
        cow.getEntityData().setLong(LAST_MILKED_TICK_KEY, now);
    }
}
