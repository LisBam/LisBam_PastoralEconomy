package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.equipment.ShoulderEquipmentService;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/** Applies tracked-player shoulder snapshots on the physical client's main thread. */
public final class ClientShoulderEquipmentSyncExecutor {
    private ClientShoulderEquipmentSyncExecutor() {
    }

    public static void acceptServerSnapshot(final int entityId, final ItemStack stack) {
        final ItemStack snapshot = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                Minecraft minecraft = Minecraft.getMinecraft();
                if (minecraft.world == null) {
                    return;
                }
                Entity entity = minecraft.world.getEntityByID(entityId);
                if (entity instanceof EntityPlayer) {
                    ShoulderEquipmentService.setShoulderStack((EntityPlayer) entity, snapshot);
                }
            }
        });
    }
}
