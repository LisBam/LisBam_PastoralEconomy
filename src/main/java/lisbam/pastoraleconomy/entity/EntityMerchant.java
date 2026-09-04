package lisbam.pastoraleconomy.entity;

import lisbam.pastoraleconomy.merchant.MerchantTradeService;
import lisbam.pastoraleconomy.merchant.StationRole;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.EntityCreature;

import java.util.UUID;

/** A non-hostile, persistent humanoid whose logical identity lives in MerchantRecord. */
public final class EntityMerchant extends EntityCreature {
    private static final String KEY_MERCHANT_ID = "merchantId";
    private static final String KEY_VILLAGE_ID = "villageId";
    private static final String KEY_STATION_ID = "stationId";
    private static final String KEY_STATION_X = "stationX";
    private static final String KEY_STATION_Y = "stationY";
    private static final String KEY_STATION_Z = "stationZ";
    private static final int HOME_RADIUS = 12;
    private static final int RETURN_DISTANCE = 32;
    private static final int RETURN_REPATH_INTERVAL_TICKS = 20;

    private UUID merchantId;
    private UUID villageId;
    private UUID stationId;
    private BlockPos stationPosition;
    /** Runtime-only pathfinding throttle; station identity remains fully persisted above. */
    private int nextReturnPathTick;

    public EntityMerchant(World worldIn) {
        super(worldIn);
        setSize(0.6F, 1.95F);
        enablePersistence();
        setCustomNameTag("商人");
        setAlwaysRenderNameTag(false);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 0.5D));
        tasks.addTask(6, new EntityAIWanderAvoidWater(this, 0.5D));
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
    }

    public UUID getMerchantId() { return merchantId; }
    public UUID getVillageId() { return villageId; }
    public UUID getStationId() { return stationId; }
    public boolean hasValidBinding() { return merchantId != null && villageId != null && stationId != null && stationPosition != null; }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote || stationPosition == null) {
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
    }
}
