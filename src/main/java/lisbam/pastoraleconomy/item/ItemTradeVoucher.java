package lisbam.pastoraleconomy.item;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.UUID;

/** A one-use blank voucher whose bound UUID authorizes loaded vanilla chest stock during sales. */
public final class ItemTradeVoucher extends Item {
    private static final String DATA_TAG = LisBamPastoralEconomy.MODID;
    private static final String KEY_OWNER = "VoucherOwner";
    private static final String KEY_OWNER_NAME = "VoucherOwnerName";
    private static final int MAX_STORED_NAME_LENGTH = 64;

    public ItemTradeVoucher() {
        setRegistryName(LisBamPastoralEconomy.MODID, "trade_voucher");
        setUnlocalizedName(LisBamPastoralEconomy.MODID + ".trade_voucher");
        setCreativeTab(ModItems.CREATIVE_TAB);
        setMaxStackSize(1);
        addPropertyOverride(new ResourceLocation("bound"), new IItemPropertyGetter() {
            @Override
            public float apply(ItemStack stack, World world, EntityLivingBase entity) {
                return isBound(stack) ? 1.0F : 0.0F;
            }
        });
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (isBound(held)) {
            return new ActionResult<ItemStack>(EnumActionResult.PASS, held);
        }
        if (!world.isRemote && bind(held, player.getUniqueID(), player.getName())) {
            player.inventory.markDirty();
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
        }
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, held);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (!isBound(stack)) {
            return super.getItemStackDisplayName(stack);
        }
        String ownerName = getBoundPlayerName(stack);
        return I18n.translateToLocalFormatted(
                "item.lisbam_pastoral_economy.trade_voucher.bound.name",
                ownerName.isEmpty() ? "?" : ownerName).trim();
    }

    /** Writes the immutable first owner while preserving unrelated ItemStack NBT. */
    public static boolean bind(ItemStack stack, UUID ownerId, String ownerName) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemTradeVoucher)
                || ownerId == null || isBound(stack)) {
            return false;
        }
        NBTTagCompound data = stack.getOrCreateSubCompound(DATA_TAG);
        data.setUniqueId(KEY_OWNER, ownerId);
        String safeName = ownerName == null ? "" : ownerName.trim();
        if (safeName.length() > MAX_STORED_NAME_LENGTH) {
            safeName = safeName.substring(0, MAX_STORED_NAME_LENGTH);
        }
        data.setString(KEY_OWNER_NAME, safeName);
        return true;
    }

    public static boolean isBound(ItemStack stack) {
        return getBoundPlayerId(stack) != null;
    }

    public static boolean isBoundTo(ItemStack stack, UUID ownerId) {
        UUID stored = getBoundPlayerId(stack);
        return ownerId != null && ownerId.equals(stored);
    }

    public static UUID getBoundPlayerId(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemTradeVoucher)) {
            return null;
        }
        NBTTagCompound data = stack.getSubCompound(DATA_TAG);
        return data != null && data.hasUniqueId(KEY_OWNER) ? data.getUniqueId(KEY_OWNER) : null;
    }

    public static String getBoundPlayerName(ItemStack stack) {
        NBTTagCompound data = stack == null ? null : stack.getSubCompound(DATA_TAG);
        return data == null ? "" : data.getString(KEY_OWNER_NAME);
    }
}
