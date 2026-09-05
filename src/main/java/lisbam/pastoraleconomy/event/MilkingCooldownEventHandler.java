package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.config.ModSettings;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
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

        // The logical server owns the timer. Client interaction is left to
        // vanilla so its prediction and inventory synchronization remain
        // exactly the normal cow-milking path.
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

        // Other interaction handlers have now had a chance to cancel. An adult
        // non-creative player holding a vanilla bucket is the exact branch
        // EntityCow.processInteract will successfully milk. Record the server
        // timestamp, then let that original branch create the genuine milk
        // bucket and perform all inventory synchronization.
        cow.getEntityData().setLong(LAST_MILKED_TICK_KEY, now);
    }
}
