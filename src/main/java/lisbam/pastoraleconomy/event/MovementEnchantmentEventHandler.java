package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;

/** Server-owned movement attributes and the level-I farmland damage prevention. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class MovementEnchantmentEventHandler {
    private static final UUID FLEETFOOT_ID = UUID.fromString("8b63e3f5-a1d4-49d9-a10c-ff1f410d6be1");
    private static final UUID FARMLAND_WALKER_ID = UUID.fromString("39590944-3bd5-4621-9578-aa8a4614ed3f");

    private MovementEnchantmentEventHandler() {
    }

    /** Client FOV compensation needs the exact non-persistent attribute identity. */
    public static UUID getFleetfootModifierId() {
        return FLEETFOOT_ID;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntity();
        ItemStack boots = player.getItemStackFromSlot(EntityEquipmentSlot.FEET);
        if (ModEnchantments.isBoots(boots)
                && EnchantmentHelper.getEnchantmentLevel(ModEnchantments.FARMLAND_WALKER, boots) > 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void updateMovementAttributes(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        ItemStack leggings = player.getItemStackFromSlot(EntityEquipmentSlot.LEGS);
        int fleetfootLevel = ModEnchantments.isLeggings(leggings)
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.FLEETFOOT, leggings) : 0;
        ItemStack boots = player.getItemStackFromSlot(EntityEquipmentSlot.FEET);
        int farmlandWalkerLevel = ModEnchantments.isBoots(boots)
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.FARMLAND_WALKER, boots) : 0;
        // Keep Fleetfoot's modifier stable through jumps. Removing it on the
        // airborne tick creates a client attribute-sync window that vanilla
        // turns into a one-frame FOV change.
        boolean fleetfootActive = fleetfootLevel > 0;
        boolean groundTravel = player.onGround && !player.isRiding() && !player.capabilities.isFlying;
        boolean onFarmland = groundTravel
                && player.world.getBlockState(player.getPosition().down()).getBlock() == Blocks.FARMLAND;

        IAttributeInstance movement = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        setModifier(movement, FLEETFOOT_ID, "LisBam Fleetfoot", fleetfootActive,
                fleetfootLevel * 0.20D);
        double walkerAmount = farmlandWalkerLevel == 2 ? 0.20D : farmlandWalkerLevel >= 3 ? 0.35D : 0.0D;
        setModifier(movement, FARMLAND_WALKER_ID, "LisBam Farmland Walker", onFarmland && walkerAmount > 0.0D,
                walkerAmount);
    }

    private static void setModifier(IAttributeInstance attribute, UUID id, String name, boolean shouldApply, double amount) {
        AttributeModifier existing = attribute.getModifier(id);
        if (!shouldApply) {
            if (existing != null) {
                attribute.removeModifier(existing);
            }
            return;
        }
        if (existing != null && existing.getAmount() == amount && existing.getOperation() == 2) {
            return;
        }
        if (existing != null) {
            attribute.removeModifier(existing);
        }
        attribute.applyModifier(new AttributeModifier(id, name, amount, 2).setSaved(false));
    }
}
