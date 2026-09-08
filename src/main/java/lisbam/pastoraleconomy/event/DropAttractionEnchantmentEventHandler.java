package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
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

/** Makes confirmed harvest and direct-melee mob drops fly to the actual server-side player owner. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class DropAttractionEnchantmentEventHandler {
    private static final Map<BreakKey, BreakAction> PENDING_BREAKS = new HashMap<BreakKey, BreakAction>();

    private DropAttractionEnchantmentEventHandler() {
    }

    /** Capture before vanilla durability can break the tool, but only after ordinary protection checks. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureToolBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote || event.isCanceled()) {
            return;
        }
        EntityPlayer player = event.getPlayer();
        if (player == null || player instanceof FakePlayer) {
            return;
        }
        ItemStack tool = player.getHeldItemMainhand();
        if (!ModEnchantments.isDropAttractionItem(tool)
                || EnchantmentHelper.getEnchantmentLevel(ModEnchantments.DROP_ATTRACTION, tool) <= 0) {
            return;
        }
        PENDING_BREAKS.put(new BreakKey(event.getWorld(), event.getPos()),
                new BreakAction(player, event.getWorld().getTotalWorldTime() + 1L));
    }

    /** Replace vanilla spawning only after every normal drop modifier has finalized the actual stack list. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void spawnAttractedHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        BreakAction action = PENDING_BREAKS.remove(new BreakKey(event.getWorld(), event.getPos()));
        if (action == null || action.player.isDead || action.player.world != event.getWorld()) {
            return;
        }
        List<ItemStack> drops = new ArrayList<ItemStack>();
        for (ItemStack stack : event.getDrops()) {
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        if (drops.isEmpty()) {
            return;
        }
        event.getDrops().clear();
        for (ItemStack stack : drops) {
            // HarvestDropsEvent normally rolls this chance for every final stack. Reproduce that policy after
            // replacing native spawning, so Fortune/Silk Touch/protection modifiers retain their semantics.
            if (event.getWorld().rand.nextFloat() > event.getDropChance()) {
                continue;
            }
            EntityItem item = new EntityItem(event.getWorld(), event.getPos().getX() + 0.5D,
                    event.getPos().getY() + 0.5D, event.getPos().getZ() + 0.5D, stack);
            item.setNoPickupDelay();
            if (event.getWorld().spawnEntity(item)) {
                DropAttractionService.attract(item, action.player);
            }
        }
    }

    /**
     * Existing death drops have already been assembled by vanilla and higher-priority Forge handlers. Marking
     * those same entities preserves Looting, modded drops and the original pickup path without duplicating loot.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attractDirectMeleeLivingDrops(LivingDropsEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        if (event.isCanceled() || victim.world.isRemote || victim instanceof EntityPlayer) {
            return;
        }
        EntityPlayer player = getDirectPlayer(event.getSource());
        if (player == null || player instanceof FakePlayer) {
            return;
        }
        ItemStack weapon = player.getHeldItemMainhand();
        if (!ModEnchantments.isDropAttractionItem(weapon)
                || EnchantmentHelper.getEnchantmentLevel(ModEnchantments.DROP_ATTRACTION, weapon) <= 0) {
            return;
        }
        for (EntityItem item : event.getDrops()) {
            DropAttractionService.attract(item, player);
        }
    }

    /** Protection or another mod can cancel a break after capture; discard its one-tick intent without retaining player data. */
    @SubscribeEvent
    public static void discardExpiredBreaks(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_BREAKS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BreakKey, BreakAction>> iterator = PENDING_BREAKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BreakKey, BreakAction> entry = iterator.next();
            BreakKey key = entry.getKey();
            if (key.world.getTotalWorldTime() >= entry.getValue().expiresAt) {
                iterator.remove();
            }
        }
    }

    private static EntityPlayer getDirectPlayer(DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity immediate = source.getImmediateSource();
        Entity trueSource = source.getTrueSource();
        return immediate == trueSource && immediate instanceof EntityPlayer ? (EntityPlayer) immediate : null;
    }

    private static final class BreakAction {
        private final EntityPlayer player;
        private final long expiresAt;

        private BreakAction(EntityPlayer player, long expiresAt) {
            this.player = player;
            this.expiresAt = expiresAt;
        }
    }

    private static final class BreakKey {
        private final World world;
        private final BlockPos position;

        private BreakKey(World world, BlockPos position) {
            this.world = world;
            this.position = position.toImmutable();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof BreakKey)) {
                return false;
            }
            BreakKey that = (BreakKey) other;
            return world == that.world && position.equals(that.position);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(world) * 31 + position.hashCode();
        }
    }
}
