package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.event.MovementEnchantmentEventHandler;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Keeps Fleetfoot's movement-speed modifier out of the local FOV calculation. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID, value = Side.CLIENT)
public final class ClientFleetfootFovHandler {
    private ClientFleetfootFovHandler() {
    }

    @SubscribeEvent
    public static void removeFleetfootFovBoost(EntityViewRenderEvent.FOVModifier event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntity();
        IAttributeInstance movement = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        AttributeModifier fleetfoot = movement.getModifier(MovementEnchantmentEventHandler.getFleetfootModifierId());
        if (fleetfoot == null || fleetfoot.getOperation() != 2 || fleetfoot.getAmount() <= 0.0D) {
            return;
        }
        double actualSpeed = movement.getAttributeValue();
        double walkSpeed = player.capabilities.getWalkSpeed();
        if (actualSpeed <= 0.0D || walkSpeed <= 0.0D) {
            return;
        }

        // AbstractClientPlayer multiplies FOV by (speed / walkSpeed + 1) / 2.
        // Recompute the attribute without this UUID instead of dividing the
        // final value: this remains correct with other operation-0/1/2 speed
        // modifiers and during the equipment/jump synchronization boundary.
        double normalSpeed = getSpeedWithoutFleetfoot(movement);
        double normalTerm = (normalSpeed / walkSpeed + 1.0D) * 0.5D;
        double actualTerm = (actualSpeed / walkSpeed + 1.0D) * 0.5D;
        if (actualTerm > 0.0D) {
            event.setFOV((float) (event.getFOV() * normalTerm / actualTerm));
        }
    }

    private static double getSpeedWithoutFleetfoot(IAttributeInstance movement) {
        double value = movement.getBaseValue();
        for (AttributeModifier modifier : movement.getModifiersByOperation(0)) {
            if (!MovementEnchantmentEventHandler.getFleetfootModifierId().equals(modifier.getID())) {
                value += modifier.getAmount();
            }
        }
        double operationOneBase = value;
        for (AttributeModifier modifier : movement.getModifiersByOperation(1)) {
            if (!MovementEnchantmentEventHandler.getFleetfootModifierId().equals(modifier.getID())) {
                value += operationOneBase * modifier.getAmount();
            }
        }
        for (AttributeModifier modifier : movement.getModifiersByOperation(2)) {
            if (!MovementEnchantmentEventHandler.getFleetfootModifierId().equals(modifier.getID())) {
                value *= 1.0D + modifier.getAmount();
            }
        }
        return value;
    }
}
