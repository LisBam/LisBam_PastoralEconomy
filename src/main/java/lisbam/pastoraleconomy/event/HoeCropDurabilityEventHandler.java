package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.agriculture.AgricultureRules;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Adds the one durability point that ItemHoe omits for zero-hardness crops. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class HoeCropDurabilityEventHandler {
    private static final List<PendingHoeWear> PENDING_HOE_WEAR = new ArrayList<PendingHoeWear>();

    private HoeCropDurabilityEventHandler() {
    }

    /** HarvestDropsEvent proves the server successfully removed and harvested this block. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureSuccessfulCropHarvest(BlockEvent.HarvestDropsEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        EntityPlayer harvester = event.getHarvester();
        if (!(harvester instanceof EntityPlayerMP) || harvester instanceof FakePlayer) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) harvester;
        ItemStack hoe = player.getHeldItemMainhand();
        float hardness = event.getState().getBlockHardness(event.getWorld(), event.getPos());
        if (!shouldQueueHoeWear(hoe, event.getState(), hardness, player.capabilities.isCreativeMode)) {
            return;
        }
        PENDING_HOE_WEAR.add(new PendingHoeWear(
                player, event.getWorld(), event.getPos(), player.inventory.currentItem, hoe));
    }

    /**
     * Damage and synchronize after PlayerInteractionManager, Block#harvestBlock and every
     * HarvestDropsEvent listener have finished. This keeps inventory mutation out of the
     * vanilla drop call and re-broadcasts the server's final crop/air state to all trackers.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyConfirmedHoeWear(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_HOE_WEAR.isEmpty()) {
            return;
        }
        Iterator<PendingHoeWear> iterator = PENDING_HOE_WEAR.iterator();
        while (iterator.hasNext()) {
            PendingHoeWear wear = iterator.next();
            iterator.remove();
            wear.applyAndSynchronize();
        }
    }

    static boolean shouldQueueHoeWear(ItemStack tool, IBlockState state, float hardness, boolean creativeMode) {
        return !creativeMode && ModEnchantments.isHoe(tool)
                && AgricultureRules.shouldConsumeHoeCropDurability(state, hardness);
    }

    private static final class PendingHoeWear {
        private final EntityPlayerMP player;
        private final World world;
        private final BlockPos pos;
        private final int hotbarSlot;
        private final ItemStack hoe;

        private PendingHoeWear(EntityPlayerMP player, World world, BlockPos pos,
                               int hotbarSlot, ItemStack hoe) {
            this.player = player;
            this.world = world;
            this.pos = pos.toImmutable();
            this.hotbarSlot = hotbarSlot;
            this.hoe = hoe;
        }

        private void applyAndSynchronize() {
            IBlockState finalState = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, finalState, finalState, 3);

            if (player.world != world || player.capabilities.isCreativeMode
                    || player.inventory.currentItem != hotbarSlot
                    || player.inventory.mainInventory.get(hotbarSlot) != hoe
                    || hoe.isEmpty()) {
                return;
            }

            ItemStack beforeDamage = hoe.copy();
            hoe.damageItem(1, player);
            if (hoe.isEmpty()) {
                ForgeEventFactory.onPlayerDestroyItem(player, beforeDamage, EnumHand.MAIN_HAND);
            }
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
        }
    }
}
