package lisbam.pastoraleconomy.entity;

import com.google.common.base.Predicate;
import lisbam.pastoraleconomy.merchant.MerchantNameGenerator;
import lisbam.pastoraleconomy.merchant.MerchantTradeService;
import lisbam.pastoraleconomy.merchant.StationRole;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.EntityCreature;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A non-hostile, persistent humanoid whose logical identity lives in MerchantRecord. */
public final class EntityMerchant extends EntityCreature {
    private static final String KEY_MERCHANT_ID = "merchantId";
    private static final String KEY_VILLAGE_ID = "villageId";
    private static final String KEY_STATION_ID = "stationId";
    private static final String KEY_STATION_X = "stationX";
    private static final String KEY_STATION_Y = "stationY";
    private static final String KEY_STATION_Z = "stationZ";
    private static final String KEY_SKIN_VARIANT = "merchantSkin";
    /** Old releases wrote this generic custom name; it is treated as unnamed during migration. */
    private static final String LEGACY_GENERIC_NAME = "商人";
    private static final DataParameter<Integer> SKIN_VARIANT = EntityDataManager.createKey(EntityMerchant.class,
            DataSerializers.VARINT);
    private static final int HOME_RADIUS = 12;
    private static final int RETURN_DISTANCE = 32;
    private static final int RETURN_REPATH_INTERVAL_TICKS = 20;
    private static final int FLEE_DURATION_TICKS = 200;
    private static final float FLEE_DISTANCE = 12.0F;
    private static final float PLAYER_LOOK_DISTANCE = 8.0F;

    private UUID merchantId;
    private UUID villageId;
    private UUID stationId;
    private BlockPos stationPosition;
    /** False only until an old NBT record or a new server spawn receives its persistent selection. */
    private boolean skinVariantAssigned;
    /** Runtime-only pathfinding throttle; station identity remains fully persisted above. */
    private int nextReturnPathTick;
    /** Runtime-only open-trade ownership; it must never affect entity NBT or persistence. */
    private final Set<UUID> tradingPlayerIds = new HashSet<UUID>();
    private UUID fleeingPlayerId;
    private int fleeUntilTick;

    public EntityMerchant(World worldIn) {
        super(worldIn);
        setSize(0.6F, 1.95F);
        enablePersistence();
        setAlwaysRenderNameTag(false);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(SKIN_VARIANT, Integer.valueOf(MerchantSkinCatalog.DEFAULT_STEVE));
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(1, new EntityAIAvoidEntity<EntityPlayer>(this, EntityPlayer.class, new Predicate<EntityPlayer>() {
            @Override
            public boolean apply(EntityPlayer player) {
                return shouldFleeFrom(player);
            }
        }, FLEE_DISTANCE, 0.8D, 1.2D));
        tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 0.5D));
        tasks.addTask(6, new EntityAIWanderAvoidWater(this, 0.5D));
        tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, PLAYER_LOOK_DISTANCE));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(10.0D);
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    public void bind(UUID newMerchantId, UUID newVillageId, UUID newStationId, BlockPos newStationPosition) {
        if (newMerchantId == null || newVillageId == null || newStationId == null || newStationPosition == null) {
            throw new IllegalArgumentException("Merchant binding is incomplete.");
        }
        merchantId = newMerchantId;
        villageId = newVillageId;
        stationId = newStationId;
        stationPosition = newStationPosition.toImmutable();
        setHomePosAndDistance(stationPosition.up(), HOME_RADIUS);
        ensurePresentation();
    }

    public UUID getMerchantId() { return merchantId; }
    public UUID getVillageId() { return villageId; }
    public UUID getStationId() { return stationId; }
    public int getSkinVariant() { return MerchantSkinCatalog.normalize(dataManager.get(SKIN_VARIANT).intValue()); }
    public boolean hasValidBinding() { return merchantId != null && villageId != null && stationId != null && stationPosition != null; }

    /** Generates only on the logical server; DataManager handles the matching client render state. */
    private void ensurePresentation() {
        if (world.isRemote) {
            return;
        }
        String currentName = getCustomNameTag();
        if (currentName == null || currentName.trim().isEmpty() || LEGACY_GENERIC_NAME.equals(currentName)) {
            setCustomNameTag(MerchantNameGenerator.generate(world.rand));
        }
        if (!skinVariantAssigned) {
            dataManager.set(SKIN_VARIANT, Integer.valueOf(world.rand.nextInt(MerchantSkinCatalog.SKIN_COUNT)));
            skinVariantAssigned = true;
        }
    }

    /** Freezes only this merchant while one or more server-authorized trade windows are open. */
    public void beginTrading(EntityPlayer player) {
        if (!world.isRemote && player != null) {
            tradingPlayerIds.add(player.getUniqueID());
            stopMovement();
        }
    }

    /** Called from the matching server Container close lifecycle. */
    public void endTrading(EntityPlayer player) {
        if (!world.isRemote && player != null) {
            tradingPlayerIds.remove(player.getUniqueID());
        }
    }

    private boolean isTrading() {
        return !tradingPlayerIds.isEmpty();
    }

    private boolean shouldFleeFrom(EntityPlayer player) {
        if (isTrading() || player == null || fleeingPlayerId == null || ticksExisted >= fleeUntilTick) {
            return false;
        }
        return fleeingPlayerId.equals(player.getUniqueID());
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        boolean damaged = super.attackEntityFrom(source, amount);
        if (damaged && !world.isRemote) {
            Entity attacker = source.getTrueSource();
            if (attacker instanceof EntityPlayer) {
                fleeingPlayerId = attacker.getUniqueID();
                fleeUntilTick = ticksExisted + FLEE_DURATION_TICKS;
                getNavigator().clearPath();
            }
        }
        return damaged;
    }

    @Override
    protected void updateAITasks() {
        if (isTrading()) {
            stopMovement();
            return;
        }
        super.updateAITasks();
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) {
            return;
        }
        if (isTrading()) {
            stopMovement();
            lookAtNearestPlayer();
            return;
        }
        if (stationPosition == null || isFleeing()) {
            return;
        }
        if (getDistanceSq(stationPosition.getX() + 0.5D, stationPosition.getY() + 1.0D,
                stationPosition.getZ() + 0.5D) <= (double) (RETURN_DISTANCE * RETURN_DISTANCE)) {
            nextReturnPathTick = ticksExisted;
            return;
        }
        if (ticksExisted < nextReturnPathTick) {
            return;
        }
        nextReturnPathTick = ticksExisted + RETURN_REPATH_INTERVAL_TICKS;
        boolean pathing = getNavigator().tryMoveToXYZ(stationPosition.getX() + 0.5D, stationPosition.getY() + 1.0D,
                stationPosition.getZ() + 0.5D, 0.8D);
        if (!pathing && world.isBlockLoaded(stationPosition)) {
            BlockPos standing = stationPosition.up();
            if (world.isAirBlock(standing) && world.isAirBlock(standing.up())) {
                setPosition(standing.getX() + 0.5D, standing.getY(), standing.getZ() + 0.5D);
            }
        }
    }

    private boolean isFleeing() {
        if (fleeingPlayerId != null && ticksExisted < fleeUntilTick) {
            return true;
        }
        fleeingPlayerId = null;
        return false;
    }

    private void lookAtNearestPlayer() {
        EntityPlayer nearest = world.getClosestPlayerToEntity(this, PLAYER_LOOK_DISTANCE);
        if (nearest != null) {
            getLookHelper().setLookPositionWithEntity(nearest, 10.0F, getVerticalFaceSpeed());
        }
    }

    private void stopMovement() {
        getNavigator().clearPath();
        setMoveForward(0.0F);
        setMoveStrafing(0.0F);
        motionX = 0.0D;
        motionZ = 0.0D;
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (!world.isRemote) {
            MerchantTradeService.openTrade(player, this);
        }
        return true;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (merchantId != null) { compound.setUniqueId(KEY_MERCHANT_ID, merchantId); }
        if (villageId != null) { compound.setUniqueId(KEY_VILLAGE_ID, villageId); }
        if (stationId != null) { compound.setUniqueId(KEY_STATION_ID, stationId); }
        if (stationPosition != null) {
            compound.setInteger(KEY_STATION_X, stationPosition.getX());
            compound.setInteger(KEY_STATION_Y, stationPosition.getY());
            compound.setInteger(KEY_STATION_Z, stationPosition.getZ());
        }
        if (skinVariantAssigned) {
            compound.setInteger(KEY_SKIN_VARIANT, getSkinVariant());
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        merchantId = compound.hasUniqueId(KEY_MERCHANT_ID) ? compound.getUniqueId(KEY_MERCHANT_ID) : null;
        villageId = compound.hasUniqueId(KEY_VILLAGE_ID) ? compound.getUniqueId(KEY_VILLAGE_ID) : null;
        stationId = compound.hasUniqueId(KEY_STATION_ID) ? compound.getUniqueId(KEY_STATION_ID) : null;
        if (compound.hasKey(KEY_STATION_X) && compound.hasKey(KEY_STATION_Y) && compound.hasKey(KEY_STATION_Z)) {
            stationPosition = new BlockPos(compound.getInteger(KEY_STATION_X), compound.getInteger(KEY_STATION_Y),
                    compound.getInteger(KEY_STATION_Z));
            setHomePosAndDistance(stationPosition.up(), HOME_RADIUS);
        } else {
            stationPosition = null;
        }
        skinVariantAssigned = compound.hasKey(KEY_SKIN_VARIANT)
                && MerchantSkinCatalog.isValid(compound.getInteger(KEY_SKIN_VARIANT));
        if (skinVariantAssigned) {
            dataManager.set(SKIN_VARIANT, Integer.valueOf(compound.getInteger(KEY_SKIN_VARIANT)));
        }
        ensurePresentation();
    }
}
