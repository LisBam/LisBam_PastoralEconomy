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
        // Remove only Fleetfoot's contribution so bow, flying, and other effects
        // retain their normal vanilla FOV behavior.
        double normalSpeed = actualSpeed / (1.0D + fleetfoot.getAmount());
        double normalTerm = (normalSpeed / walkSpeed + 1.0D) * 0.5D;
        double actualTerm = (actualSpeed / walkSpeed + 1.0D) * 0.5D;
        if (actualTerm > 0.0D) {
            event.setFOV((float) (event.getFOV() * normalTerm / actualTerm));
        }
    }
}
