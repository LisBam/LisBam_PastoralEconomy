package lisbam.pastoraleconomy.shipping;

import lisbam.pastoraleconomy.data.player.CoinService;
import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import lisbam.pastoraleconomy.item.ItemTradeVoucher;
import lisbam.pastoraleconomy.market.MarketPriceSnapshot;
import lisbam.pastoraleconomy.market.MarketService;
import lisbam.pastoraleconomy.merchant.TradeCatalog;
import lisbam.pastoraleconomy.merchant.TradeCatalogEntry;
import lisbam.pastoraleconomy.tile.TileShippingBox;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-only daily shipping settlement for every currently loaded shipping box. */
public final class ShippingBoxService {
    private ShippingBoxService() {
    }

    /**
     * Runs from the overworld's logical-server tick. One persisted marker
     * makes all loaded boxes one market-day batch, even over restarts.
     */
    public static void tick(WorldServer overworld) {
        if (overworld == null || overworld.isRemote || overworld.provider.getDimension() != 0) {
            return;
        }
        PastoralWorldData data = PastoralWorldData.get(overworld);
        ShippingBoxPayoutState payouts = data.getShippingBoxPayoutState();
        long worldDay = PastoralWorldData.getAuthoritativeMarketDay(overworld);
        if (payouts.tryBeginDispatch(worldDay)) {
            MarketPriceSnapshot prices = MarketService.getPriceSnapshot(overworld);
            for (TileShippingBox box : collectLoadedShippingBoxes(overworld)) {
                SalePlan plan = createSalePlan(box, prices);
                if (plan == null || !payouts.canCredit(plan.allocations)) {
                    continue;
                }
                if (!payouts.credit(plan.allocations)) {
                    throw new IllegalStateException("Validated shipping-box payout allocation was rejected.");
                }
                applySalePlan(box, plan);
            }
            data.markDirty();
        }
        // Keep delayed offline or maximum-balance payouts available without
        // scanning every saved UUID; only currently connected players are read.
        if (overworld.getTotalWorldTime() % 20L == 0L) {
            flushOnlinePayouts(overworld);
        }
    }

    /** Called from login synchronization so an offline recipient is paid immediately on return. */
    public static void flushPayoutForPlayer(EntityPlayerMP player) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        PastoralWorldData data = PastoralWorldData.get(player.world);
        ShippingBoxPayoutState payouts = data.getShippingBoxPayoutState();
        long pending = payouts.getPending(player.getUniqueID());
        if (pending <= 0L) {
            return;
        }
        long room = Long.MAX_VALUE - CoinService.getBalance(player);
        long payment = Math.min(pending, room);
        if (payment <= 0L || !CoinService.addCoins(player, payment) || !payouts.claim(player.getUniqueID(), payment)) {
            return;
        }
        data.markDirty();
        player.sendMessage(new TextComponentString(TextFormatting.GREEN
                + "【出货箱】今日收益+" + payment + "金币！"));
    }

    private static void flushOnlinePayouts(WorldServer overworld) {
        MinecraftServer server = overworld.getMinecraftServer();
        if (server == null || server.getPlayerList() == null) {
            return;
        }
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            flushPayoutForPlayer(player);
        }
    }

    private static List<TileShippingBox> collectLoadedShippingBoxes(WorldServer overworld) {
        MinecraftServer server = overworld.getMinecraftServer();
        if (server == null || server.worlds == null) {
            return Collections.emptyList();
        }
        List<TileShippingBox> result = new ArrayList<TileShippingBox>();
        for (WorldServer world : server.worlds) {
            if (world == null) {
                continue;
            }
            for (TileEntity tile : new ArrayList<TileEntity>(world.loadedTileEntityList)) {
                if (tile instanceof TileShippingBox && !tile.isInvalid()) {
                    result.add((TileShippingBox) tile);
                }
            }
        }
        return result;
    }

    /** Package-private deterministic seam used by the shipping-box self-test. */
    static SalePlan createSalePlan(TileShippingBox box, MarketPriceSnapshot prices) {
        if (box == null || prices == null) {
            return null;
        }
        LinkedHashSet<UUID> recipients = new LinkedHashSet<UUID>();
        for (int slot = 0; slot < box.getSizeInventory(); slot++) {
            UUID owner = ItemTradeVoucher.getBoundPlayerId(box.getStackInSlot(slot));
            if (owner != null) {
                recipients.add(owner);
            }
        }
        if (recipients.isEmpty()) {
            return null;
        }

        long grossIncome = 0L;
        List<SoldStack> soldStacks = new ArrayList<SoldStack>();
        for (int slot = 0; slot < box.getSizeInventory(); slot++) {
            ItemStack stack = box.getStackInSlot(slot);
            TradeCatalogEntry entry = TradeCatalog.findSellEntry(stack);
            if (entry == null) {
                continue;
            }
            Long unitPrice = prices.getCurrentPrice(entry.getMarketKey());
            long stackGross = unitPrice == null ? -1L
                    : ShippingBoxRules.calculateGrossIncome(unitPrice.longValue(), stack.getCount());
            grossIncome = ShippingBoxRules.addIncome(grossIncome, stackGross);
            if (grossIncome < 0L) {
                return null;
            }
            soldStacks.add(new SoldStack(slot, stack.copy(), isMilkBucket(entry)));
        }
        if (soldStacks.isEmpty()) {
            return null;
        }
        long discountedIncome = ShippingBoxRules.calculateDiscountedIncome(grossIncome);
        Map<UUID, Long> allocations = ShippingBoxRules.splitEvenly(discountedIncome, recipients);
        return allocations.isEmpty() ? null : new SalePlan(soldStacks, allocations);
    }

    private static boolean isMilkBucket(TradeCatalogEntry entry) {
        return entry != null && entry.getMarketKey().endsWith("sell/livestock/milk_bucket");
    }

    private static void applySalePlan(TileShippingBox box, SalePlan plan) {
        for (SoldStack sold : plan.soldStacks) {
            ItemStack current = box.getStackInSlot(sold.slot);
            if (!ItemStack.areItemStacksEqual(current, sold.stack)) {
                throw new IllegalStateException("Shipping-box inventory changed during its atomic daily settlement.");
            }
            box.setInventorySlotContents(sold.slot, sold.milkBucket
                    ? new ItemStack(Items.BUCKET, sold.stack.getCount(), 0) : ItemStack.EMPTY);
        }
        box.markDirty();
    }

    static final class SalePlan {
        private final List<SoldStack> soldStacks;
        private final Map<UUID, Long> allocations;

        private SalePlan(List<SoldStack> soldStacks, Map<UUID, Long> allocations) {
            this.soldStacks = Collections.unmodifiableList(new ArrayList<SoldStack>(soldStacks));
            this.allocations = Collections.unmodifiableMap(new LinkedHashMap<UUID, Long>(allocations));
        }

        Map<UUID, Long> getAllocations() {
            return allocations;
        }

        int getSoldStackCount() {
            return soldStacks.size();
        }
    }

    private static final class SoldStack {
        private final int slot;
        private final ItemStack stack;
        private final boolean milkBucket;

        private SoldStack(int slot, ItemStack stack, boolean milkBucket) {
            this.slot = slot;
            this.stack = stack;
            this.milkBucket = milkBucket;
        }
    }
}
