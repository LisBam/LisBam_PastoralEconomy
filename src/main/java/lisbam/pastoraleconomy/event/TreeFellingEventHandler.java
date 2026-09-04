package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.block.BlockNewLeaf;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Conservative vanilla-tree felling. Only a bounded same-species log component
 * with a matching leaf crown qualifies; leaves are scanned once and never used
 * to traverse to another tree.
 */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class TreeFellingEventHandler {
    private static final int LOG_HORIZONTAL_RADIUS = 8;
    private static final int LOG_VERTICAL_RADIUS = 32;
    private static final int MAX_LOGS = 256;
    private static final int LEAF_MARGIN = 5;
    private static final Set<UUID> ACTIVE_FELLING = new HashSet<UUID>();

    private TreeFellingEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fellNaturalVanillaTree(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote || event.isCanceled() || !(event.getPlayer() instanceof EntityPlayerMP)
                || event.getPlayer() instanceof FakePlayer) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getPlayer();
        UUID playerId = player.getUniqueID();
        if (ACTIVE_FELLING.contains(playerId)) {
            return;
        }
        ItemStack tool = player.getHeldItemMainhand();
        if (!ModEnchantments.isAxe(tool)
                || EnchantmentHelper.getEnchantmentLevel(ModEnchantments.FELLING, tool) <= 0) {
            return;
        }

        TreePlan plan = TreePlan.find(event.getWorld(), event.getPos(), event.getState());
        if (plan == null) {
            return;
        }

        ACTIVE_FELLING.add(playerId);
        try {
            for (BlockPos target : plan.getSecondaryTargets(event.getPos())) {
                if (player.getHeldItemMainhand().isEmpty()) {
                    break;
                }
                // This is the native server harvest path: it posts BreakEvent for
                // every secondary block, honors protection cancellations, creates
                // normal drops, and consumes vanilla durability one block at a time.
                player.interactionManager.tryHarvestBlock(target);
            }
        } finally {
            ACTIVE_FELLING.remove(playerId);
        }
    }

    private static final class TreePlan {
        private final BlockPlanks.EnumType woodType;
        private final Set<BlockPos> logs;
        private final List<BlockPos> leaves;

        private TreePlan(BlockPlanks.EnumType woodType, Set<BlockPos> logs, List<BlockPos> leaves) {
            this.woodType = woodType;
            this.logs = logs;
            this.leaves = leaves;
        }

        private static TreePlan find(World world, BlockPos origin, IBlockState originState) {
            BlockPlanks.EnumType woodType = getVanillaWoodType(originState);
            if (woodType == null) {
                return null;
            }
            Set<BlockPos> logs = collectConnectedLogs(world, origin, woodType);
            if (logs.size() < 2 || logs.size() > MAX_LOGS || !hasMatchingCrown(world, logs, woodType)) {
                return null;
            }
            List<BlockPos> leaves = collectOwnLeaves(world, logs, woodType);
            if (leaves.isEmpty()) {
                return null;
            }
            return new TreePlan(woodType, logs, leaves);
        }

        private List<BlockPos> getSecondaryTargets(BlockPos origin) {
            List<BlockPos> targets = new ArrayList<BlockPos>();
            List<BlockPos> orderedLogs = new ArrayList<BlockPos>(logs);
            Collections.sort(orderedLogs, DESCENDING_Y);
            for (BlockPos log : orderedLogs) {
                if (!log.equals(origin)) {
                    targets.add(log);
                }
            }
            targets.addAll(leaves);
            return targets;
        }

        private static Set<BlockPos> collectConnectedLogs(World world, BlockPos origin, BlockPlanks.EnumType woodType) {
            Set<BlockPos> result = new HashSet<BlockPos>();
            ArrayDeque<BlockPos> open = new ArrayDeque<BlockPos>();
            result.add(origin.toImmutable());
            open.add(origin.toImmutable());
            while (!open.isEmpty() && result.size() <= MAX_LOGS) {
                BlockPos current = open.removeFirst();
                for (int xOffset = -1; xOffset <= 1; xOffset++) {
                    for (int yOffset = -1; yOffset <= 1; yOffset++) {
                        for (int zOffset = -1; zOffset <= 1; zOffset++) {
                            if (xOffset == 0 && yOffset == 0 && zOffset == 0) {
                                continue;
                            }
                            BlockPos candidate = current.add(xOffset, yOffset, zOffset);
                            if (Math.abs(candidate.getX() - origin.getX()) > LOG_HORIZONTAL_RADIUS
                                    || Math.abs(candidate.getZ() - origin.getZ()) > LOG_HORIZONTAL_RADIUS
                                    || Math.abs(candidate.getY() - origin.getY()) > LOG_VERTICAL_RADIUS
                                    || result.contains(candidate) || !world.isBlockLoaded(candidate)) {
                                continue;
                            }
                            if (woodType == getVanillaWoodType(world.getBlockState(candidate))) {
                                BlockPos immutable = candidate.toImmutable();
                                result.add(immutable);
                                open.addLast(immutable);
                            }
                        }
                    }
                }
            }
            return result;
        }

        private static boolean hasMatchingCrown(World world, Set<BlockPos> logs, BlockPlanks.EnumType woodType) {
            for (BlockPos log : logs) {
                if (logs.contains(log.up())) {
                    continue;
                }
                int matchingLeaves = 0;
                for (int xOffset = -4; xOffset <= 4; xOffset++) {
                    for (int yOffset = -2; yOffset <= 4; yOffset++) {
                        for (int zOffset = -4; zOffset <= 4; zOffset++) {
                            BlockPos candidate = log.add(xOffset, yOffset, zOffset);
                            if (world.isBlockLoaded(candidate)
                                    && woodType == getVanillaLeafType(world.getBlockState(candidate))) {
                                matchingLeaves++;
                                if (matchingLeaves >= 8) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }

        private static List<BlockPos> collectOwnLeaves(World world, Set<BlockPos> logs, BlockPlanks.EnumType woodType) {
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (BlockPos log : logs) {
                minX = Math.min(minX, log.getX());
                maxX = Math.max(maxX, log.getX());
                minY = Math.min(minY, log.getY());
                maxY = Math.max(maxY, log.getY());
                minZ = Math.min(minZ, log.getZ());
                maxZ = Math.max(maxZ, log.getZ());
            }

            List<BlockPos> foreignLogs = findForeignLogs(world, logs, woodType,
                    minX - LEAF_MARGIN, maxX + LEAF_MARGIN, minY - 3, maxY + LEAF_MARGIN,
                    minZ - LEAF_MARGIN, maxZ + LEAF_MARGIN);
            List<BlockPos> leaves = new ArrayList<BlockPos>();
            for (int x = minX - LEAF_MARGIN; x <= maxX + LEAF_MARGIN; x++) {
                for (int y = minY - 3; y <= maxY + LEAF_MARGIN; y++) {
                    for (int z = minZ - LEAF_MARGIN; z <= maxZ + LEAF_MARGIN; z++) {
                        BlockPos candidate = new BlockPos(x, y, z);
                        if (!world.isBlockLoaded(candidate)
                                || woodType != getVanillaLeafType(world.getBlockState(candidate))
                                || !isCloserToTargetTrunk(candidate, logs, foreignLogs)) {
                            continue;
                        }
                        leaves.add(candidate);
                    }
                }
            }
            Collections.sort(leaves, DESCENDING_Y);
            return leaves;
        }

        private static List<BlockPos> findForeignLogs(World world, Set<BlockPos> targetLogs,
                                                      BlockPlanks.EnumType woodType, int minX, int maxX,
                                                      int minY, int maxY, int minZ, int maxZ) {
            List<BlockPos> foreignLogs = new ArrayList<BlockPos>();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos candidate = new BlockPos(x, y, z);
                        if (world.isBlockLoaded(candidate) && !targetLogs.contains(candidate)
                                && woodType == getVanillaWoodType(world.getBlockState(candidate))) {
                            foreignLogs.add(candidate);
                        }
                    }
                }
            }
            return foreignLogs;
        }

        private static boolean isCloserToTargetTrunk(BlockPos leaf, Set<BlockPos> targetLogs,
                                                      List<BlockPos> foreignLogs) {
            int targetDistance = closestHorizontalDistanceSquared(leaf, targetLogs);
            int foreignDistance = closestHorizontalDistanceSquared(leaf, foreignLogs);
            return targetDistance <= LEAF_MARGIN * LEAF_MARGIN && targetDistance < foreignDistance;
        }

        private static int closestHorizontalDistanceSquared(BlockPos source, Iterable<BlockPos> targets) {
            int nearest = Integer.MAX_VALUE;
            for (BlockPos target : targets) {
                int xDistance = source.getX() - target.getX();
                int zDistance = source.getZ() - target.getZ();
                nearest = Math.min(nearest, xDistance * xDistance + zDistance * zDistance);
            }
            return nearest;
        }

        private static BlockPlanks.EnumType getVanillaWoodType(IBlockState state) {
            if (state.getBlock() == Blocks.LOG) {
                return state.getValue(BlockOldLog.VARIANT);
            }
            if (state.getBlock() == Blocks.LOG2) {
                return state.getValue(BlockNewLog.VARIANT);
            }
            return null;
        }

        private static BlockPlanks.EnumType getVanillaLeafType(IBlockState state) {
            if (state.getBlock() == Blocks.LEAVES) {
                return state.getValue(BlockOldLeaf.VARIANT);
            }
            if (state.getBlock() == Blocks.LEAVES2) {
                return state.getValue(BlockNewLeaf.VARIANT);
            }
            return null;
        }
    }

    private static final Comparator<BlockPos> DESCENDING_Y = new Comparator<BlockPos>() {
        @Override
        public int compare(BlockPos first, BlockPos second) {
            int yComparison = Integer.compare(second.getY(), first.getY());
            if (yComparison != 0) {
                return yComparison;
            }
            int xComparison = Integer.compare(first.getX(), second.getX());
            return xComparison != 0 ? xComparison : Integer.compare(first.getZ(), second.getZ());
        }
    };
}
