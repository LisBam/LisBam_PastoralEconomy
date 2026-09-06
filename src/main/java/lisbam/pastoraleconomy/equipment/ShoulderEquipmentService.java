package lisbam.pastoraleconomy.equipment;

import lisbam.pastoraleconomy.data.player.IPlayerData;
import lisbam.pastoraleconomy.data.player.PlayerDataCapability;
import lisbam.pastoraleconomy.item.ItemBackpack;
import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.network.message.SyncShoulderEquipmentMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Common-side ownership boundary for the one persistent shoulder slot.  The
 * Object bridge intentionally keeps the Coremod injection descriptor free of
 * MCP/SRG class names.
 */
public final class ShoulderEquipmentService {
    private static final Map<UUID, ItemStack> LAST_SYNCED_STACKS = new HashMap<UUID, ItemStack>();

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
            if (player instanceof EntityPlayerMP && !player.world.isRemote) {
                syncIfChanged((EntityPlayerMP) player);
            }
        }
    }

    public static boolean isValidShoulderStack(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.getItem() == Items.ELYTRA || ItemBackpack.isBackpack(stack));
    }

    /** Sends equipment changes to the wearer and every client tracking that player. */
    public static void syncIfChanged(EntityPlayerMP player) {
        if (player == null || player.world.isRemote) {
            return;
        }
        ItemStack current = normalizedCopy(getShoulderStack(player));
        ItemStack previous = LAST_SYNCED_STACKS.get(player.getUniqueID());
        if (previous != null && ItemStack.areItemStacksEqual(previous, current)) {
            return;
        }
        syncNow(player);
    }

    /** Forced lifecycle snapshot for login, respawn and dimension replacement. */
    public static void syncNow(EntityPlayerMP player) {
        if (player == null || player.world.isRemote) {
            return;
        }
        ItemStack current = normalizedCopy(getShoulderStack(player));
        LAST_SYNCED_STACKS.put(player.getUniqueID(), current.copy());
        ModNetwork.CHANNEL.sendTo(new SyncShoulderEquipmentMessage(player.getEntityId(), current), player);
        // Backpacks intentionally have no worn model. Do not disclose or resend
        // their complete storage NBT to unrelated tracking clients.
        ModNetwork.CHANNEL.sendToAllTracking(new SyncShoulderEquipmentMessage(
                player.getEntityId(), renderingCopy(current)), player);
    }

    /** Initial snapshot for a newly tracking client; this does not alter the global change cache. */
    public static void syncToPlayer(EntityPlayerMP receiver, EntityPlayer target) {
        if (receiver == null || target == null || receiver.world.isRemote) {
            return;
        }
        ItemStack current = normalizedCopy(getShoulderStack(target));
        ModNetwork.CHANNEL.sendTo(new SyncShoulderEquipmentMessage(
                target.getEntityId(), receiver == target ? current : renderingCopy(current)), receiver);
    }

    public static void forgetSyncedState(EntityPlayer player) {
        if (player != null) {
            LAST_SYNCED_STACKS.remove(player.getUniqueID());
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

    private static ItemStack normalizedCopy(ItemStack stack) {
        if (!isValidShoulderStack(stack)) {
            return ItemStack.EMPTY;
        }
        ItemStack normalized = stack.copy();
        normalized.setCount(1);
        return normalized;
    }

    private static ItemStack renderingCopy(ItemStack stack) {
        return stack.getItem() == Items.ELYTRA ? stack : ItemStack.EMPTY;
    }
}
