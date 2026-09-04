package lisbam.pastoraleconomy.entity.ai;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * A low-frequency, server-only movement task. It never impersonates a player
 * or invokes block drops; a success is exactly one direct block-state change.
 */
public final class EntityAIFarmHarassment extends EntityAIBase {
    private static final int CHECK_MIN_TICKS = 200;
    private static final int CHECK_MAX_TICKS = 320;
    private static final int SUCCESS_COOLDOWN_MIN_TICKS = 900;
    private static final int SUCCESS_COOLDOWN_MAX_TICKS = 1500;
    private static final int SEARCH_HORIZONTAL_RADIUS = 8;
    private static final int SEARCH_VERTICAL_RADIUS = 3;
    private static final double ACTION_DISTANCE_SQ = 3.24D;
    private static final double PLAYER_ENABLE_DISTANCE_SQ = 48.0D * 48.0D;
    private static final double MOVE_SPEED = 1.0D;

    private static final String PERSISTENT_ROOT = LisBamPastoralEconomy.MODID + ":farm_harassment";
    private static final String KEY_NEXT_CHECK_TICK = "nextCheckTick";

    private final EntityLiving mob;
    private BlockPos target;
    private boolean cropTarget;

    public EntityAIFarmHarassment(EntityLiving mob) {
        this.mob = mob;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (mob.getAttackTarget() != null) {
            return false;
        }

        long now = getAuthoritativeTick();
        if (now < getNextCheckTick(now)) {
            return false;
        }

        scheduleNextCandidateCheck(now);
        if (!hasNearbyLivingPlayer() || !isMobGriefingEnabled()) {
            return false;
        }

        boolean lookForCrop = mob.getRNG().nextInt(4) != 0;
        target = findRandomTarget(lookForCrop);
        cropTarget = lookForCrop;
        return target != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (target == null || mob.getAttackTarget() != null || !hasNearbyLivingPlayer() || !isTargetStillValid()) {
            return false;
        }
        return isWithinActionDistance() || !mob.getNavigator().noPath();
    }

    @Override
    public void startExecuting() {
        if (target == null || !mob.getNavigator().tryMoveToXYZ(
                target.getX() + 0.5D,
                target.getY() + 0.5D,
                target.getZ() + 0.5D,
                MOVE_SPEED
        )) {
            target = null;
        }
    }

    @Override
    public void resetTask() {
        target = null;
        cropTarget = false;
        mob.getNavigator().clearPath();
    }

    @Override
    public void updateTask() {
        if (target == null || mob.getAttackTarget() != null || !hasNearbyLivingPlayer() || !isTargetStillValid()) {
            abandonTarget();
            return;
        }

        if (!isWithinActionDistance()) {
            if (mob.getNavigator().noPath()) {
                abandonTarget();
            }
            return;
        }

        if (!isMobGriefingEnabled()) {
            abandonTarget();
            return;
        }

        long now = getAuthoritativeTick();
        PastoralWorldData worldData = PastoralWorldData.get(mob.world);
        if (!worldData.canRecordFarmHarassment(mob.world.provider.getDimension(), target, now)) {
            abandonTarget();
            return;
        }

        if (!changeTargetBlock()) {
            abandonTarget();
            return;
        }

        worldData.recordFarmHarassment(mob.world.provider.getDimension(), target, now);
        setNextCheckTick(now + randomInRange(SUCCESS_COOLDOWN_MIN_TICKS, SUCCESS_COOLDOWN_MAX_TICKS));
        abandonTarget();
    }

    private BlockPos findRandomTarget(boolean lookForCrop) {
        World world = mob.world;
        List<BlockPos> candidates = new ArrayList<BlockPos>();
        int centerX = MathHelper.floor(mob.posX);
        int centerY = MathHelper.floor(mob.posY);
        int centerZ = MathHelper.floor(mob.posZ);

        for (int x = centerX - SEARCH_HORIZONTAL_RADIUS; x <= centerX + SEARCH_HORIZONTAL_RADIUS; x++) {
            for (int y = centerY - SEARCH_VERTICAL_RADIUS; y <= centerY + SEARCH_VERTICAL_RADIUS; y++) {
                for (int z = centerZ - SEARCH_HORIZONTAL_RADIUS; z <= centerZ + SEARCH_HORIZONTAL_RADIUS; z++) {
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (!world.isBlockLoaded(candidate)) {
                        continue;
                    }
                    if (lookForCrop ? isValidCropTarget(candidate) : isValidEmptyFarmlandTarget(candidate)) {
                        candidates.add(candidate);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(mob.getRNG().nextInt(candidates.size()));
    }

    private boolean isTargetStillValid() {
        return cropTarget ? isValidCropTarget(target) : isValidEmptyFarmlandTarget(target);
    }

    private boolean isValidCropTarget(BlockPos position) {
        World world = mob.world;
        if (!world.isBlockLoaded(position) || !world.isBlockLoaded(position.down())) {
            return false;
        }
        IBlockState cropState = world.getBlockState(position);
        return isListedCrop(cropState.getBlock())
                && world.getBlockState(position.down()).getBlock() == Blocks.FARMLAND;
    }

    private boolean isValidEmptyFarmlandTarget(BlockPos position) {
        World world = mob.world;
        if (!world.isBlockLoaded(position) || !world.isBlockLoaded(position.up())) {
            return false;
        }
        return world.getBlockState(position).getBlock() == Blocks.FARMLAND && world.isAirBlock(position.up());
    }

    private boolean changeTargetBlock() {
        if (cropTarget && isValidCropTarget(target)) {
            return mob.world.setBlockState(target, Blocks.AIR.getDefaultState(), 3);
        }
        if (!cropTarget && isValidEmptyFarmlandTarget(target)) {
            return mob.world.setBlockState(target, Blocks.DIRT.getDefaultState(), 3);
        }
        return false;
    }

    private boolean hasNearbyLivingPlayer() {
        for (EntityPlayer player : mob.world.playerEntities) {
            if (player.isEntityAlive() && mob.getDistanceSq(player) <= PLAYER_ENABLE_DISTANCE_SQ) {
                return true;
            }
        }
        return false;
    }

    private boolean isWithinActionDistance() {
        return mob.getDistanceSq(
                target.getX() + 0.5D,
                target.getY() + 0.5D,
                target.getZ() + 0.5D
        ) <= ACTION_DISTANCE_SQ;
    }

    private boolean isMobGriefingEnabled() {
        return mob.world.getGameRules().getBoolean("mobGriefing");
    }

    private long getNextCheckTick(long now) {
        NBTTagCompound persistentData = getPersistentData();
        if (!persistentData.hasKey(KEY_NEXT_CHECK_TICK)) {
            long nextCheck = now + randomInRange(CHECK_MIN_TICKS, CHECK_MAX_TICKS);
            persistentData.setLong(KEY_NEXT_CHECK_TICK, nextCheck);
            return nextCheck;
        }
        return persistentData.getLong(KEY_NEXT_CHECK_TICK);
    }

    private void scheduleNextCandidateCheck(long now) {
        setNextCheckTick(now + randomInRange(CHECK_MIN_TICKS, CHECK_MAX_TICKS));
    }

    private void setNextCheckTick(long nextCheckTick) {
        getPersistentData().setLong(KEY_NEXT_CHECK_TICK, nextCheckTick);
    }

    private NBTTagCompound getPersistentData() {
        NBTTagCompound entityData = mob.getEntityData();
        NBTBase existing = entityData.getTag(PERSISTENT_ROOT);
        if (!(existing instanceof NBTTagCompound)) {
            entityData.setTag(PERSISTENT_ROOT, new NBTTagCompound());
        }
        return entityData.getCompoundTag(PERSISTENT_ROOT);
    }

    private int randomInRange(int minimum, int maximum) {
        return minimum + mob.getRNG().nextInt(maximum - minimum + 1);
    }

    private void abandonTarget() {
        target = null;
        cropTarget = false;
        mob.getNavigator().clearPath();
    }

    private long getAuthoritativeTick() {
        return PastoralWorldData.getAuthoritativeTick(mob.world);
    }

    private static boolean isListedCrop(Block block) {
        return block == Blocks.WHEAT
                || block == Blocks.CARROTS
                || block == Blocks.POTATOES
                || block == Blocks.BEETROOTS
                || block == Blocks.PUMPKIN_STEM
                || block == Blocks.MELON_STEM;
    }
}
