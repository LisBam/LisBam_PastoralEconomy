package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.equipment.FeatherWingsRules;
import lisbam.pastoraleconomy.equipment.ShoulderEquipmentService;
import lisbam.pastoraleconomy.item.ItemFeatherWings;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Logical-server authority for Feather Wings flight, durability and hunger. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class FeatherWingsFlightEventHandler {
    private static final Map<UUID, FlightState> FLIGHT_STATES = new HashMap<UUID, FlightState>();
    /** Only revoke the ability when this handler, rather than another mod, granted it. */
    private static final Set<UUID> GRANTED_FLIGHT = new HashSet<UUID>();

    private FeatherWingsFlightEventHandler() {
    }

    @SubscribeEvent
    public static void updateFeatherWingsFlight(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote
                || !(event.player instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        ItemStack shoulder = ShoulderEquipmentService.getShoulderStack(player);
        boolean hasFeatherWings = ItemFeatherWings.isFeatherWings(shoulder);
        boolean hasVanillaFlight = player.capabilities.isCreativeMode || player.isSpectator();
        if (!hasFeatherWings) {
            revokeGrantedFlight(player, hasVanillaFlight);
            FLIGHT_STATES.remove(player.getUniqueID());
            return;
        }

        if (!hasVanillaFlight && !player.capabilities.allowFlying) {
            player.capabilities.allowFlying = true;
            GRANTED_FLIGHT.add(player.getUniqueID());
            player.sendPlayerAbilities();
        }

        if (hasVanillaFlight || !player.capabilities.isFlying) {
            if (hasVanillaFlight) {
                GRANTED_FLIGHT.remove(player.getUniqueID());
            }
            FLIGHT_STATES.remove(player.getUniqueID());
            return;
        }

        FlightState state = getOrCreateState(player);
        state.addMovementExhaustion(player);
        state.flightTicks++;
        if (state.flightTicks < FeatherWingsRules.FLIGHT_TICKS_PER_DURABILITY) {
            return;
        }

        state.flightTicks = 0;
        shoulder.damageItem(1, player);
        if (shoulder.isEmpty()) {
            ShoulderEquipmentService.setShoulderStack(player, ItemStack.EMPTY);
            revokeGrantedFlight(player, false);
            FLIGHT_STATES.remove(player.getUniqueID());
        } else {
            ShoulderEquipmentService.syncIfChanged(player);
        }
    }

    @SubscribeEvent
    public static void forgetFlightState(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player != null) {
            FLIGHT_STATES.remove(event.player.getUniqueID());
            GRANTED_FLIGHT.remove(event.player.getUniqueID());
        }
    }

    /**
     * The shoulder stack is outside vanilla's armor/hand equipment scan, so
     * consume the orb here only when its Mending enchantment can actually
     * repair these wings. The remainder still becomes ordinary player XP.
     */
    @SubscribeEvent
    public static void mendShoulderWings(PlayerPickupXpEvent event) {
        if (!(event.getEntityPlayer() instanceof EntityPlayerMP) || event.getEntityPlayer().world.isRemote) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        ItemStack shoulder = ShoulderEquipmentService.getShoulderStack(player);
        if (!ItemFeatherWings.isFeatherWings(shoulder) || !shoulder.isItemDamaged()
                || EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, shoulder) <= 0) {
            return;
        }

        EntityXPOrb orb = event.getOrb();
        float ratio = shoulder.getItem().getXpRepairRatio(shoulder);
        int repaired = Math.min(Math.round(orb.xpValue * ratio), shoulder.getItemDamage());
        if (repaired <= 0) {
            return;
        }
        int spentExperience = Math.min(orb.xpValue, Math.max(1, Math.round(repaired / ratio)));
        shoulder.setItemDamage(shoulder.getItemDamage() - repaired);
        event.setCanceled(true);
        player.onItemPickup(orb, 1);
        if (orb.xpValue > spentExperience) {
            player.addExperience(orb.xpValue - spentExperience);
        }
        orb.setDead();
        ShoulderEquipmentService.syncIfChanged(player);
    }

    private static FlightState getOrCreateState(EntityPlayerMP player) {
        UUID id = player.getUniqueID();
        FlightState state = FLIGHT_STATES.get(id);
        if (state == null) {
            state = new FlightState(player.posX, player.posY, player.posZ);
            FLIGHT_STATES.put(id, state);
        }
        return state;
    }

    private static void revokeGrantedFlight(EntityPlayerMP player, boolean hasVanillaFlight) {
        if (!GRANTED_FLIGHT.remove(player.getUniqueID()) || hasVanillaFlight
                || (!player.capabilities.allowFlying && !player.capabilities.isFlying)) {
            return;
        }
        player.capabilities.isFlying = false;
        player.capabilities.allowFlying = false;
        player.sendPlayerAbilities();
    }

    private static final class FlightState {
        private int flightTicks;
        private double lastX;
        private double lastY;
        private double lastZ;

        private FlightState(double x, double y, double z) {
            lastX = x;
            lastY = y;
            lastZ = z;
        }

        private void addMovementExhaustion(EntityPlayerMP player) {
            double x = player.posX;
            double y = player.posY;
            double z = player.posZ;
            int distanceCentimeters = (int) Math.round(Math.sqrt(
                    (x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) + (z - lastZ) * (z - lastZ)
            ) * 100.0D);
            player.addExhaustion(FeatherWingsRules.getFlightExhaustion(distanceCentimeters));
            lastX = x;
            lastY = y;
            lastZ = z;
        }
    }
}
