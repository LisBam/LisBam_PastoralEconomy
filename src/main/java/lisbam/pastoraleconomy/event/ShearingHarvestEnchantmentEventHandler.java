package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.agriculture.AgricultureRules;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Adds Harvest's bonus only after native sheep/mooshroom shearing succeeds. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class ShearingHarvestEnchantmentEventHandler {
    private static final Map<UUID, PendingShear> PENDING_SHEARS = new HashMap<UUID, PendingShear>();

    private ShearingHarvestEnchantmentEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureShearingAttempt(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote || event.isCanceled()) {
            return;
        }
        ItemStack shears = event.getItemStack();
        Entity target = event.getTarget();
        int level = shears.getItem() == Items.SHEARS
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.HARVEST, shears) : 0;
        int attractionLevel = shears.getItem() == Items.SHEARS
                ? EnchantmentHelper.getEnchantmentLevel(ModEnchantments.DROP_ATTRACTION, shears) : 0;
        if ((level <= 0 && attractionLevel <= 0) || !(target instanceof EntitySheep || target instanceof EntityMooshroom)) {
            return;
        }
        if (target instanceof EntitySheep && ((EntitySheep) target).getSheared()) {
            return;
        }
        BlockPos pos = new BlockPos(target);
        if (!(target instanceof IShearable) || !((IShearable) target).isShearable(shears, event.getWorld(), pos)) {
            return;
        }
        PENDING_SHEARS.put(target.getUniqueID(), new PendingShear(event.getWorld(), target, event.getEntityPlayer(),
                level, attractionLevel));
    }

    /** Native shearing spawns one entity per returned stack, so tag those exact server-side entities as they join. */
    @SubscribeEvent
    public static void attractConfirmedShearingDrops(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityItem)) {
            return;
        }
        EntityItem item = (EntityItem) event.getEntity();
        for (PendingShear action : PENDING_SHEARS.values()) {
            if (action.shouldAttract(item)) {
                DropAttractionService.attract(item, action.player);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void applyConfirmedShearingBonus(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_SHEARS.isEmpty()) {
            return;
        }
        Iterator<PendingShear> pending = PENDING_SHEARS.values().iterator();
        while (pending.hasNext()) {
            PendingShear action = pending.next();
            pending.remove();
            if (!action.wasSuccessful()) {
                continue;
            }
            int bonus = AgricultureRules.rollShearingHarvestBonus(action.level, action.world.rand);
            if (bonus <= 0) {
                continue;
            }
            EntityItem entityItem = new EntityItem(action.world, action.target.posX, action.target.posY,
                    action.target.posZ, action.createBonusStack(bonus));
            entityItem.motionY += action.world.rand.nextFloat() * 0.05F;
            entityItem.motionX += (action.world.rand.nextFloat() - action.world.rand.nextFloat()) * 0.1F;
            entityItem.motionZ += (action.world.rand.nextFloat() - action.world.rand.nextFloat()) * 0.1F;
            if (action.world.spawnEntity(entityItem) && action.attractionLevel > 0) {
                DropAttractionService.attract(entityItem, action.player);
            }
        }
    }

    private static final class PendingShear {
        private final World world;
        private final Entity target;
        private final net.minecraft.entity.player.EntityPlayer player;
        private final int level;
        private final int attractionLevel;
        private final boolean sheep;
        private final int woolMetadata;

        private PendingShear(World world, Entity target, net.minecraft.entity.player.EntityPlayer player,
                             int level, int attractionLevel) {
            this.world = world;
            this.target = target;
            this.player = player;
            this.level = level;
            this.attractionLevel = attractionLevel;
            this.sheep = target instanceof EntitySheep;
            this.woolMetadata = sheep ? ((EntitySheep) target).getFleeceColor().getMetadata() : 0;
        }

        private boolean wasSuccessful() {
            return sheep ? target instanceof EntitySheep && ((EntitySheep) target).getSheared() : target.isDead;
        }

        private boolean shouldAttract(EntityItem item) {
            if (attractionLevel <= 0 || !wasSuccessful() || item.world != world || player == null || player.isDead) {
                return false;
            }
            double x = item.posX - target.posX;
            double y = item.posY - target.posY;
            double z = item.posZ - target.posZ;
            return x * x + y * y + z * z <= 9.0D;
        }

        private ItemStack createBonusStack(int amount) {
            return sheep ? new ItemStack(Blocks.WOOL, amount, woolMetadata) : new ItemStack(Blocks.RED_MUSHROOM, amount);
        }
    }
}
