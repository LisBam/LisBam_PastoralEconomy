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

/**
 * Keeps vanilla-bucket milking server-authoritative.
 *
 * <p>The client never predicts the inventory change. With cooldown enabled,
 * this handler completes the server transaction; with it disabled, the server
 * continues into EntityCow's unmodified vanilla branch.</p>
 */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class MilkingCooldownEventHandler {
    private static final String LAST_MILKED_DAY_KEY = "lisbam_pastoral_economy_last_milked_day";

    private MilkingCooldownEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
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

        // PlayerControllerMP sends CPacketUseEntity before locally processing
        // this event. Always suppress its inventory prediction, including when
        // the local config differs from a multiplayer server's configuration.
        // The server's actual result will synchronize the held stack.
        if (MilkCooldownRules.shouldCancelLocalPrediction(event.getWorld().isRemote)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
            return;
        }

        boolean cooldownDisabled = ModSettings.isDisableMilkingCooldown();
        if (!MilkCooldownRules.shouldTakeOwnership(cooldownDisabled)) {
            return;
        }

        long currentWorldDay = MilkCooldownRules.getWorldDay(event.getWorld().getWorldTime());
        long lastMilkedDay = cow.getEntityData().hasKey(LAST_MILKED_DAY_KEY)
                ? cow.getEntityData().getLong(LAST_MILKED_DAY_KEY) : -1L;
        boolean onCooldown = MilkCooldownRules.isOnCooldown(lastMilkedDay, currentWorldDay);
        if (MilkCooldownRules.shouldReject(cooldownDisabled, onCooldown)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.FAIL);
            return;
        }

        boolean transactionSucceeded = completeServerMilking(event);
        if (MilkCooldownRules.shouldRecordSuccess(cooldownDisabled, onCooldown,
                transactionSucceeded)) {
            cow.getEntityData().setLong(LAST_MILKED_DAY_KEY, currentWorldDay);
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    /** Mirrors EntityCow.processInteract's successful vanilla bucket branch. */
    private static boolean completeServerMilking(PlayerInteractEvent.EntityInteract event) {
        ItemStack bucket = event.getItemStack();
        EnumHand hand = event.getHand();

        event.getEntityPlayer().playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
        bucket.shrink(1);
        if (bucket.isEmpty()) {
            event.getEntityPlayer().setHeldItem(hand, new ItemStack(Items.MILK_BUCKET));
            return true;
        }

        ItemStack milk = new ItemStack(Items.MILK_BUCKET);
        if (!event.getEntityPlayer().inventory.addItemStackToInventory(milk)) {
            event.getEntityPlayer().dropItem(milk, false);
        }
        return true;
    }
}
