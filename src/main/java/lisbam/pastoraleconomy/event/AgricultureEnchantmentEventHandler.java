package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.agriculture.AgricultureRules;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * One server-authoritative harvest pipeline. Capturing before the vanilla tool
 * durability step preserves the action's enchantment levels even if its hoe
 * breaks while harvesting the mature crop.
 */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class AgricultureEnchantmentEventHandler {
    private static final Map<UUID, HarvestAction> PENDING_ACTIONS = new HashMap<UUID, HarvestAction>();
    private static final List<PendingReplant> PENDING_REPLANTS = new ArrayList<PendingReplant>();
    private static final List<PendingCropSync> PENDING_CROP_SYNCS = new ArrayList<PendingCropSync>();

    private AgricultureEnchantmentEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureManualHarvest(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote || event.isCanceled()) {
            return;
        }
        EntityPlayer player = event.getPlayer();
        if (player == null || player instanceof FakePlayer) {
            return;
        }

        PENDING_ACTIONS.remove(player.getUniqueID());
        AgricultureRules.Crop crop = AgricultureRules.getMatureHarvestCrop(event.getState());
        if (crop == null) {
            return;
        }

        ItemStack tool = player.getHeldItemMainhand();
        int harvestLevel = (ModEnchantments.isHoe(tool) || ModEnchantments.isAxe(tool))
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.HARVEST, tool) : 0;
        int fineCultivationLevel = ModEnchantments.isHoe(tool)
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.FINE_CULTIVATION, tool) : 0;
        ItemStack helmet = player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD);
        int pastoralFavorLevel = ModEnchantments.isHelmet(helmet)
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.PASTORAL_FAVOR, helmet) : 0;
        if (harvestLevel > 0 || fineCultivationLevel > 0 || pastoralFavorLevel > 0) {
            PENDING_ACTIONS.put(player.getUniqueID(), new HarvestAction(
                    event.getWorld(), event.getPos(), event.getState(), crop,
                    harvestLevel, fineCultivationLevel, pastoralFavorLevel
            ));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyHarvestPipeline(BlockEvent.HarvestDropsEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        EntityPlayer player = event.getHarvester();
        if (player == null || player instanceof FakePlayer) {
            return;
        }
        HarvestAction action = PENDING_ACTIONS.remove(player.getUniqueID());
        if (action == null || !action.matches(event)) {
            return;
        }

        Random random = event.getWorld().rand;
        if (action.harvestLevel > 0) {
            ItemStack bonus = AgricultureRules.createHarvestBonus(action.crop, action.harvestLevel, random);
            if (!bonus.isEmpty()) {
                event.getDrops().add(bonus);
            }
        }

        if (action.fineCultivationLevel > 0 && AgricultureRules.supportsFineCultivation(action.crop)
                && AgricultureRules.rollFineCultivation(action.fineCultivationLevel, random)) {
            Item seed = AgricultureRules.getReplantSeed(action.crop);
            IBlockState replantState = AgricultureRules.getFineCultivationReplantState(action.crop);
            if (seed != null && replantState != null && consumeSeed(event.getDrops(), player, seed)) {
                PENDING_REPLANTS.add(new PendingReplant(event.getWorld(), event.getPos(), replantState));
            }
        }

        if (action.pastoralFavorLevel > 0
                && AgricultureRules.rollPastoralFavor(action.pastoralFavorLevel, random)) {
            player.addExperience(1);
        }
    }

    /** Apply and broadcast authoritative crop states after vanilla harvest/place processing has returned. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyPendingCropChanges(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || (PENDING_REPLANTS.isEmpty() && PENDING_CROP_SYNCS.isEmpty())) {
            return;
        }
        Iterator<PendingReplant> iterator = PENDING_REPLANTS.iterator();
        while (iterator.hasNext()) {
            PendingReplant replant = iterator.next();
            iterator.remove();
            replant.applyIfStillValid();
        }
        Iterator<PendingCropSync> syncIterator = PENDING_CROP_SYNCS.iterator();
        while (syncIterator.hasNext()) {
            PendingCropSync sync = syncIterator.next();
            syncIterator.remove();
            sync.broadcastFinalState();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyPastoralFavorForPlacement(BlockEvent.PlaceEvent event) {
        if (event.getWorld().isRemote || event.isCanceled()) {
            return;
        }
        EntityPlayer player = event.getPlayer();
        if (player == null || player instanceof FakePlayer
                || AgricultureRules.getPlacedCrop(event.getPlacedBlock()) == null) {
            return;
        }
        PENDING_CROP_SYNCS.add(new PendingCropSync(event.getWorld(), event.getPos()));
        ItemStack helmet = player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD);
        int level = ModEnchantments.isHelmet(helmet)
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.PASTORAL_FAVOR, helmet) : 0;
        if (AgricultureRules.rollPastoralFavor(level, event.getWorld().rand)) {
            player.addExperience(1);
        }
    }

    private static boolean consumeSeed(Iterable<ItemStack> finalDrops, EntityPlayer player, Item seed) {
        Iterator<ItemStack> dropIterator = finalDrops.iterator();
        while (dropIterator.hasNext()) {
            ItemStack drop = dropIterator.next();
            if (!drop.isEmpty() && drop.getItem() == seed) {
                drop.shrink(1);
                if (drop.isEmpty()) {
                    dropIterator.remove();
                }
                return true;
            }
        }

        for (int slot = 0; slot < player.inventory.mainInventory.size(); slot++) {
            ItemStack inventoryStack = player.inventory.mainInventory.get(slot);
            if (!inventoryStack.isEmpty() && inventoryStack.getItem() == seed) {
                inventoryStack.shrink(1);
                if (inventoryStack.isEmpty()) {
                    player.inventory.mainInventory.set(slot, ItemStack.EMPTY);
                }
                player.inventory.markDirty();
                return true;
            }
        }
        return false;
    }

    private static final class HarvestAction {
        private final net.minecraft.world.World world;
        private final BlockPos pos;
        private final IBlockState originalState;
        private final AgricultureRules.Crop crop;
        private final int harvestLevel;
        private final int fineCultivationLevel;
        private final int pastoralFavorLevel;

        private HarvestAction(net.minecraft.world.World world, BlockPos pos, IBlockState originalState,
                              AgricultureRules.Crop crop, int harvestLevel, int fineCultivationLevel,
                              int pastoralFavorLevel) {
            this.world = world;
            this.pos = pos.toImmutable();
            this.originalState = originalState;
            this.crop = crop;
            this.harvestLevel = harvestLevel;
            this.fineCultivationLevel = fineCultivationLevel;
            this.pastoralFavorLevel = pastoralFavorLevel;
        }

        private boolean matches(BlockEvent.HarvestDropsEvent event) {
            return world == event.getWorld() && pos.equals(event.getPos()) && originalState.equals(event.getState());
        }
    }

    private static final class PendingReplant {
        private final net.minecraft.world.World world;
        private final BlockPos pos;
        private final IBlockState replantState;

        private PendingReplant(net.minecraft.world.World world, BlockPos pos, IBlockState replantState) {
            this.world = world;
            this.pos = pos.toImmutable();
            this.replantState = replantState;
        }

        private void applyIfStillValid() {
            if (world.getBlockState(pos).getBlock() == Blocks.AIR
                    && world.getBlockState(pos.down()).getBlock() == Blocks.FARMLAND) {
                world.setBlockState(pos, replantState, 3);
            }
        }
    }

    private static final class PendingCropSync {
        private final net.minecraft.world.World world;
        private final BlockPos pos;

        private PendingCropSync(net.minecraft.world.World world, BlockPos pos) {
            this.world = world;
            this.pos = pos.toImmutable();
        }

        private void broadcastFinalState() {
            IBlockState finalState = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, finalState, finalState, 3);
        }
    }
}
