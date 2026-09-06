package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.equipment.ShoulderEquipmentService;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Gives supported shoulder items a server-owned right-click equip gesture. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class ShoulderEquipmentEventHandler {
    private ShoulderEquipmentEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void equipShoulderItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = event.getItemStack();
        if (!ShoulderEquipmentService.isValidShoulderStack(held)) {
            return;
        }

        ItemStack shoulder = ShoulderEquipmentService.getShoulderStack(player);
        event.setCanceled(true);
        if (!shoulder.isEmpty()) {
            event.setCancellationResult(EnumActionResult.FAIL);
            return;
        }

        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (player.world.isRemote) {
            return;
        }

        ItemStack equipped = held.copy();
        equipped.setCount(1);
        ShoulderEquipmentService.setShoulderStack(player, equipped);
        if (ShoulderEquipmentService.getShoulderStack(player).getItem() != equipped.getItem()) {
            event.setCancellationResult(EnumActionResult.FAIL);
            return;
        }
        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
            if (held.isEmpty()) {
                player.setHeldItem(event.getHand(), ItemStack.EMPTY);
            }
        }
        player.inventory.markDirty();
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        }
    }
}
