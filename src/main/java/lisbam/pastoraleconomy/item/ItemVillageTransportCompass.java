package lisbam.pastoraleconomy.item;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import lisbam.pastoraleconomy.block.ModBlocks;
import lisbam.pastoraleconomy.tile.TileVillageStation;
import lisbam.pastoraleconomy.transport.VillageTransportCompassTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemCompass;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Compass variant whose needle follows the nearest physical village transport block. */
public final class ItemVillageTransportCompass extends ItemCompass {
    private static final String TARGET_TAG = "lisbamVillageTransportTarget";
    private static final String KEY_DIMENSION = "dimension";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_Z = "z";
    private static final int REFRESH_INTERVAL = 20;

    public ItemVillageTransportCompass() {
        setRegistryName(LisBamPastoralEconomy.MODID, "village_transport_compass");
        setUnlocalizedName(LisBamPastoralEconomy.MODID + ".village_transport_compass");
        setCreativeTab(ModItems.CREATIVE_TAB);
        // Replaces ItemCompass's spawn-angle property while retaining its native wobble behavior.
        addPropertyOverride(new ResourceLocation("angle"), new VillageTransportCompassAngle());
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        if (world.isRemote || !(entity instanceof EntityPlayer)
                || (!isSelected && ((EntityPlayer) entity).getHeldItemOffhand() != stack)) {
            return;
        }
        if (entity.ticksExisted % REFRESH_INTERVAL != 0) {
            return;
        }
        PastoralWorldData data = PastoralWorldData.get(world);
        BlockPos nearest = VillageTransportCompassTarget.findNearest(
                data.getMerchantWorldState().getStations(), findLoadedVillageStationBlocks(world),
                world.provider.getDimension(), entity.getPosition());
        writeTarget(stack, world.provider.getDimension(), nearest);
    }

    private static boolean hasTarget(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey(TARGET_TAG, 10);
    }

    /**
     * Tile entities are the physical source of truth for blocks currently
     * loaded around players.  The merchant station records cover known village
     * stations at a distance without requiring a TransportWorldState entry.
     */
    private static List<BlockPos> findLoadedVillageStationBlocks(World world) {
        List<BlockPos> positions = new ArrayList<BlockPos>();
        for (net.minecraft.tileentity.TileEntity tile :
                new ArrayList<net.minecraft.tileentity.TileEntity>(world.loadedTileEntityList)) {
            if (tile instanceof TileVillageStation && world.getBlockState(tile.getPos()).getBlock()
                    == ModBlocks.VILLAGE_STATION) {
                positions.add(tile.getPos().toImmutable());
            }
        }
        return positions;
    }

    private static void writeTarget(ItemStack stack, int dimension, BlockPos target) {
        NBTTagCompound root = stack.getTagCompound();
        if (target == null) {
            if (root != null) {
                root.removeTag(TARGET_TAG);
            }
            return;
        }
        if (root == null) {
            root = new NBTTagCompound();
            stack.setTagCompound(root);
        }
        NBTTagCompound stored = new NBTTagCompound();
        stored.setInteger(KEY_DIMENSION, dimension);
        stored.setInteger(KEY_X, target.getX());
        stored.setInteger(KEY_Y, target.getY());
        stored.setInteger(KEY_Z, target.getZ());
        root.setTag(TARGET_TAG, stored);
    }

    private static final class VillageTransportCompassAngle implements IItemPropertyGetter {
        private double rotation;
        private double rota;
        private long lastUpdateTick;

        @Override
        public float apply(ItemStack stack, World world, EntityLivingBase entity) {
            if (entity == null && !stack.isOnItemFrame()) {
                return 0.0F;
            }
            boolean held = entity != null;
            Entity targetEntity = held ? entity : stack.getItemFrame();
            if (world == null) {
                world = targetEntity.world;
            }
            if (!hasTargetInWorld(stack, world)) {
                return (float) Math.random();
            }
            double yaw = held ? targetEntity.rotationYaw : getFrameRotation((EntityItemFrame) targetEntity);
            double compassAngle = getTargetToAngle(stack, targetEntity) / (Math.PI * 2.0D);
            // This is ItemCompass$1's exact 1.12.2 relation, with spawn
            // direction replaced by the stored village-station direction.
            double value = 0.5D - (MathHelper.positiveModulo(yaw / 360.0D, 1.0D)
                    - 0.25D - compassAngle);
            if (held) {
                value = wobble(world, value);
            }
            return MathHelper.positiveModulo((float) value, 1.0F);
        }

        private static boolean hasTargetInWorld(ItemStack stack, World world) {
            if (world == null || !stack.hasTagCompound()) {
                return false;
            }
            NBTTagCompound root = stack.getTagCompound();
            if (!root.hasKey(TARGET_TAG, 10)) {
                return false;
            }
            return root.getCompoundTag(TARGET_TAG).getInteger(KEY_DIMENSION) == world.provider.getDimension();
        }

        private static double getFrameRotation(EntityItemFrame frame) {
            return MathHelper.wrapDegrees(180 + frame.facingDirection.getHorizontalIndex() * 90);
        }

        private static double getTargetToAngle(ItemStack stack, Entity entity) {
            NBTTagCompound target = stack.getTagCompound().getCompoundTag(TARGET_TAG);
            return Math.atan2(target.getInteger(KEY_Z) - entity.posZ, target.getInteger(KEY_X) - entity.posX);
        }

        private double wobble(World world, double value) {
            if (world.getTotalWorldTime() != lastUpdateTick) {
                lastUpdateTick = world.getTotalWorldTime();
                double difference = MathHelper.positiveModulo(value - rotation + 0.5D, 1.0D) - 0.5D;
                rota += difference * 0.1D;
                rota *= 0.8D;
                rotation = MathHelper.positiveModulo(rotation + rota, 1.0D);
            }
            return rotation;
        }
    }
}
