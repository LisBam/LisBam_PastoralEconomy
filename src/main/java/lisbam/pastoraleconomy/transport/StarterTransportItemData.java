package lisbam.pastoraleconomy.transport;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

/** Namespaced, server-created provenance marker for the one starter station item. */
public final class StarterTransportItemData {
    private static final String ROOT_KEY = LisBamPastoralEconomy.MODID + ":transport";
    private static final String KEY_STARTER = "starter";
    private static final String KEY_OWNER = "starterOwner";

    private StarterTransportItemData() {
    }

    public static ItemStack createStarterStack(UUID owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Starter transport owner is required.");
        }
        ItemStack stack = new ItemStack(ModItems.TRANSPORT_STATION_ITEM);
        markStarter(stack, owner);
        return stack;
    }

    public static void markStarter(ItemStack stack, UUID owner) {
        if (stack == null || stack.isEmpty() || owner == null) {
            throw new IllegalArgumentException("Starter transport data is incomplete.");
        }
        NBTTagCompound root = stack.getTagCompound();
        if (root == null) {
            root = new NBTTagCompound();
            stack.setTagCompound(root);
        }
        NBTTagCompound transport = new NBTTagCompound();
        transport.setBoolean(KEY_STARTER, true);
        transport.setUniqueId(KEY_OWNER, owner);
        root.setTag(ROOT_KEY, transport);
    }

    public static UUID getStarterOwner(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound root = stack.getTagCompound();
        if (!root.hasKey(ROOT_KEY, 10)) {
            return null;
        }
        NBTTagCompound transport = root.getCompoundTag(ROOT_KEY);
        return transport.getBoolean(KEY_STARTER) && transport.hasUniqueId(KEY_OWNER)
                ? transport.getUniqueId(KEY_OWNER) : null;
    }
}
