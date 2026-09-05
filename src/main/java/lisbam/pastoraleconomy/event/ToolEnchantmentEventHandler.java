package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;

/** Server-owned held-tool attributes. Night Vision is a client-only render state. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class ToolEnchantmentEventHandler {
    private static final UUID ATTACK_SPEED_ID = UUID.fromString("5098d917-bb85-4b09-a7da-7678c4bdc2dd");
    private static final UUID RANGE_ID = UUID.fromString("ca26a30c-1a0e-4277-a7c6-b6d1d5b969c0");
    private static final int INSTANT_ATTACK_SPEED_AMOUNT = 1024;
    private static final double RANGE_PER_LEVEL = 1.5D;

    private ToolEnchantmentEventHandler() {
    }

    @SubscribeEvent
    public static void updateToolEnchantments(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        ItemStack held = player.getHeldItemMainhand();
        int attackSpeedLevel = ModEnchantments.isWeaponOrTool(held)
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.ATTACK_SPEED, held) : 0;
        int rangeLevel = ModEnchantments.isRangeItem(held)
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.RANGE, held) : 0;

        updateAttackSpeed(player, attackSpeedLevel);
        setModifier(player.getEntityAttribute(EntityPlayer.REACH_DISTANCE), RANGE_ID, "LisBam Range",
                rangeLevel > 0, getRangeBonus(rangeLevel), 0);
    }

    /** Forge applies this attribute to both client ray selection and server entity-use validation. */
    public static double getRangeBonus(int level) {
        return Math.max(0, level) * RANGE_PER_LEVEL;
    }

    private static void updateAttackSpeed(EntityPlayer player, int level) {
        IAttributeInstance attribute = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (level <= 0) {
            setModifier(attribute, ATTACK_SPEED_ID, "LisBam Attack Speed", false, 0.0D, 0);
            return;
        }
        if (level >= ModEnchantments.ATTACK_SPEED.getMaxLevel()) {
            // 1.12.2 cooldown uses ticks. This is far above the one-tick
            // threshold, so even an immediately repeated attack is charged.
            setModifier(attribute, ATTACK_SPEED_ID, "LisBam Attack Speed", true,
                    INSTANT_ATTACK_SPEED_AMOUNT, 0);
            return;
        }
        double speedMultiplier = 1.0D / (1.0D - level * 0.20D);
        setModifier(attribute, ATTACK_SPEED_ID, "LisBam Attack Speed", true,
                speedMultiplier - 1.0D, 2);
    }

    private static void setModifier(IAttributeInstance attribute, UUID id, String name,
                                    boolean shouldApply, double amount, int operation) {
        AttributeModifier existing = attribute.getModifier(id);
        if (!shouldApply) {
            if (existing != null) {
                attribute.removeModifier(existing);
            }
            return;
        }
        if (existing != null && existing.getAmount() == amount && existing.getOperation() == operation) {
            return;
        }
        if (existing != null) {
            attribute.removeModifier(existing);
        }
        attribute.applyModifier(new AttributeModifier(id, name, amount, operation).setSaved(false));
    }
}
