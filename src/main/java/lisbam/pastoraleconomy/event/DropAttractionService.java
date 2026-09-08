package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/** Server-only motion state for item entities produced by the Drop Attraction enchantment. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class DropAttractionService {
    private static final double ARRIVAL_DISTANCE_SQUARED = 0.75D * 0.75D;
    private static final Map<EntityItem, EntityPlayer> TRACKED_DROPS =
            new IdentityHashMap<EntityItem, EntityPlayer>();

    private DropAttractionService() {
    }

    /** Called only on the logical server after a confirmed tool action has produced a real item entity. */
    public static void attract(EntityItem item, EntityPlayer player) {
        if (item == null || player == null || item.isDead || player.isDead || item.world != player.world) {
            return;
        }
        item.setNoPickupDelay();
        item.setNoGravity(true);
        TRACKED_DROPS.put(item, player);
    }

    @SubscribeEvent
    public static void moveTrackedDrops(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TRACKED_DROPS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<EntityItem, EntityPlayer>> iterator = TRACKED_DROPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<EntityItem, EntityPlayer> entry = iterator.next();
            EntityItem item = entry.getKey();
            EntityPlayer player = entry.getValue();
            if (item.isDead) {
                iterator.remove();
                continue;
            }
            if (player == null || player.isDead || item.world != player.world) {
                item.setNoGravity(false);
                iterator.remove();
                continue;
            }

            double targetX = player.posX;
            double targetY = player.posY + player.height * 0.5D;
            double targetZ = player.posZ;
            double x = targetX - item.posX;
            double y = targetY - item.posY;
            double z = targetZ - item.posZ;
            double distanceSquared = x * x + y * y + z * z;
            if (distanceSquared <= ARRIVAL_DISTANCE_SQUARED) {
                // Vanilla pickup decides whether the stack enters the inventory on the next entity tick. If it
                // cannot, the no-gravity entity remains exactly where the player was instead of flying onward.
                item.setPosition(player.posX, player.posY, player.posZ);
                item.motionX = 0.0D;
                item.motionY = 0.0D;
                item.motionZ = 0.0D;
                item.velocityChanged = true;
                iterator.remove();
                continue;
            }

            double distance = Math.sqrt(distanceSquared);
            double speed = Math.min(0.85D, Math.max(0.20D, distance * 0.24D));
            item.motionX = x / distance * speed;
            item.motionY = y / distance * speed;
            item.motionZ = z / distance * speed;
            item.velocityChanged = true;
        }
    }
}
