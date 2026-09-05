package lisbam.pastoraleconomy.equipment;

import lisbam.pastoraleconomy.data.player.IPlayerData;
import lisbam.pastoraleconomy.data.player.PlayerDataCapability;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Common-side ownership boundary for the one persistent shoulder slot.  The
 * Object bridge intentionally keeps the Coremod injection descriptor free of
 * MCP/SRG class names.
 */
public final class ShoulderEquipmentService {
    private ShoulderEquipmentService() {
    }

    public static ItemStack getShoulderStack(EntityPlayer player) {
        IPlayerData data = player == null ? null : PlayerDataCapability.get(player);
        return data == null ? ItemStack.EMPTY : data.getShoulderStack();
    }

    public static void setShoulderStack(EntityPlayer player, ItemStack stack) {
        IPlayerData data = player == null ? null : PlayerDataCapability.get(player);
        if (data != null) {
            data.setShoulderStack(stack);
        }
    }

    /**
     * Replaces vanilla's chest lookup only for players.  A chest-slot Elytra
     * deliberately behaves as absent, so other inventory paths cannot bypass
     * the dedicated shoulder slot.
     */
    public static Object getShoulderElytraOrEmpty(Object entity, Object ignoredChestStack) {
        if (!(entity instanceof EntityPlayer)) {
            return ignoredChestStack;
        }
        ItemStack shoulder = getShoulderStack((EntityPlayer) entity);
        return shoulder.getItem() == Items.ELYTRA ? shoulder : ItemStack.EMPTY;
    }

    /**
     * Moves an Elytra saved in the former chest slot into the new shoulder
     * slot.  A pre-existing shoulder item is never overwritten.
     */
    public static void migrateLegacyChestElytra(EntityPlayerMP player) {
        if (player == null || player.world.isRemote) {
            return;
        }
        ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (chest.getItem() != Items.ELYTRA) {
            return;
        }
        ItemStack shoulder = getShoulderStack(player);
        player.setItemStackToSlot(EntityEquipmentSlot.CHEST, ItemStack.EMPTY);
        if (shoulder.isEmpty()) {
            setShoulderStack(player, chest);
            return;
        }
        ItemStack returned = chest.copy();
        if (!player.inventory.addItemStackToInventory(returned) && !returned.isEmpty()) {
            player.dropItem(returned, false);
        }
    }
}
