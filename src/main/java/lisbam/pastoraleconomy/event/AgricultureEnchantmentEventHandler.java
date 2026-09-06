package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.agriculture.AgricultureRules;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
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

import java.util.HashMap;
import java.util.Iterator;
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
        ItemStack tool = player.getHeldItemMainhand();
        AgricultureRules.Crop crop = AgricultureRules.getMatureHarvestCrop(event.getState());
        boolean consumeHoeDurability = ModEnchantments.isHoe(tool) && !player.capabilities.isCreativeMode
                && AgricultureRules.shouldConsumeHoeCropDurability(event.getState(),
                event.getState().getBlockHardness(event.getWorld(), event.getPos()));
        if (crop == null && !consumeHoeDurability) {
            return;
        }

        int harvestLevel = crop != null && (ModEnchantments.isHoe(tool) || ModEnchantments.isAxe(tool))
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.HARVEST, tool) : 0;
        int fineCultivationLevel = crop != null && ModEnchantments.isHoe(tool)
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.FINE_CULTIVATION, tool) : 0;
        ItemStack helmet = player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD);
        int pastoralFavorLevel = crop != null && ModEnchantments.isHelmet(helmet)
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.PASTORAL_FAVOR, helmet) : 0;
        if (harvestLevel > 0 || fineCultivationLevel > 0 || pastoralFavorLevel > 0 || consumeHoeDurability) {
            PENDING_ACTIONS.put(player.getUniqueID(), new HarvestAction(
                    event.getWorld(), event.getPos(), event.getState(), crop,
                    harvestLevel, fineCultivationLevel, pastoralFavorLevel,
                    consumeHoeDurability ? tool.getItem() : null
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

        consumeHoeCropDurability(action, player);
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
            if (seed != null && consumeSeed(event.getDrops(), player, seed)) {
                replantIfStillValid(event.getWorld(), event.getPos(), action.originalState);
            }
        }

        if (action.pastoralFavorLevel > 0
                && AgricultureRules.rollPastoralFavor(action.pastoralFavorLevel, random)) {
            player.addExperience(1);
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

    private static void consumeHoeCropDurability(HarvestAction action, EntityPlayer player) {
        if (action.hoeItem == null || player.capabilities.isCreativeMode) {
            return;
        }
        ItemStack currentTool = player.getHeldItemMainhand();
        if (currentTool.getItem() == action.hoeItem) {
            // ItemHoe skips zero-hardness crops in 1.12.2; damageItem keeps
            // vanilla Unbreaking and break handling for the missing one point.
            currentTool.damageItem(1, player);
        }
    }

    private static void replantIfStillValid(net.minecraft.world.World world, BlockPos pos, IBlockState harvestedState) {
        if (world.getBlockState(pos).getBlock() == Blocks.AIR
                && world.getBlockState(pos.down()).getBlock() == Blocks.FARMLAND) {
            world.setBlockState(pos, harvestedState.getBlock().getDefaultState(), 3);
        }
    }

    private static final class HarvestAction {
        private final net.minecraft.world.World world;
        private final BlockPos pos;
        private final IBlockState originalState;
        private final AgricultureRules.Crop crop;
        private final int harvestLevel;
        private final int fineCultivationLevel;
        private final int pastoralFavorLevel;
        private final Item hoeItem;

        private HarvestAction(net.minecraft.world.World world, BlockPos pos, IBlockState originalState,
                              AgricultureRules.Crop crop, int harvestLevel, int fineCultivationLevel,
                              int pastoralFavorLevel, Item hoeItem) {
            this.world = world;
            this.pos = pos.toImmutable();
            this.originalState = originalState;
            this.crop = crop;
            this.harvestLevel = harvestLevel;
            this.fineCultivationLevel = fineCultivationLevel;
            this.pastoralFavorLevel = pastoralFavorLevel;
            this.hoeItem = hoeItem;
        }

        private boolean matches(BlockEvent.HarvestDropsEvent event) {
            return world == event.getWorld() && pos.equals(event.getPos()) && originalState.equals(event.getState());
        }
    }
}
