package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.data.player.CoinService;
import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import lisbam.pastoraleconomy.entity.EntityMerchant;
import lisbam.pastoraleconomy.gui.ContainerMerchantTrade;
import lisbam.pastoraleconomy.gui.GuiIds;
import lisbam.pastoraleconomy.market.MarketPriceSnapshot;
import lisbam.pastoraleconomy.market.MarketService;
import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.network.message.SyncMerchantTradeMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Server-authoritative, atomic merchant transactions and GUI snapshots. */
public final class MerchantTradeService {
    public static final int MAX_REQUEST_QUANTITY = 4096;

    private MerchantTradeService() {
    }

    public static void openTrade(EntityPlayer player, EntityMerchant merchant) {
        if (!(player instanceof EntityPlayerMP) || merchant == null || merchant.world.isRemote || merchant.isDead) {
            return;
        }
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        if (serverPlayer.getDistanceSq(merchant) > 64.0D || !ensureMerchantRecord(serverPlayer.world, merchant)) {
            return;
        }
        PastoralWorldData data = PastoralWorldData.get(serverPlayer.world);
        MerchantRecord record = data.getMerchantWorldState().getMerchant(merchant.getMerchantId());
        if (record == null || !record.isActive()) {
            return;
        }
        if (MerchantOfferService.ensureOffers(serverPlayer.world, record)) {
            data.markDirty();
        }
        serverPlayer.openGui(LisBamPastoralEconomy.INSTANCE, GuiIds.MERCHANT_TRADE, serverPlayer.world,
                merchant.getEntityId(), 0, 0);
        if (serverPlayer.openContainer instanceof ContainerMerchantTrade) {
            merchant.beginTrading(serverPlayer);
            sendSnapshot(serverPlayer);
        }
    }

    public static void handleTransaction(EntityPlayerMP player, MerchantTradeRequest request) {
        if (player == null || request == null || player.world.isRemote
                || !(player.openContainer instanceof ContainerMerchantTrade)) {
            return;
        }
        ContainerMerchantTrade container = (ContainerMerchantTrade) player.openContainer;
        if (!container.getMerchantId().equals(request.getMerchantId()) || request.getWindowId() != container.windowId
                || !container.acceptRequestId(request.getRequestId()) || container.isCommitting()) {
            sendSnapshot(player);
            return;
        }
        container.beginCommit();
        try {
            if (request.getExpectedWorldDay() < 0L || request.getQuantity() <= 0
                    || request.getQuantity() > MAX_REQUEST_QUANTITY) {
                sendSnapshot(player);
                return;
            }
            Entity entity = player.world.getEntityByID(container.getEntityId());
            if (!(entity instanceof EntityMerchant) || entity.isDead
                    || !container.canInteractWith(player)) {
                player.closeScreen();
                return;
            }
            EntityMerchant merchant = (EntityMerchant) entity;
            if (!merchant.getMerchantId().equals(request.getMerchantId()) || !ensureMerchantRecord(player.world, merchant)) {
                player.closeScreen();
                return;
            }
            PastoralWorldData data = PastoralWorldData.get(player.world);
            MerchantRecord record = data.getMerchantWorldState().getMerchant(merchant.getMerchantId());
            if (record == null || !record.isActive()) {
                player.closeScreen();
                return;
            }
            boolean regenerated = MerchantOfferService.ensureOffers(player.world, record);
            if (regenerated) {
                data.markDirty();
            }
            DailyOfferState offers = record.getDailyOfferState();
            if (offers == null || offers.getWorldDay() != request.getExpectedWorldDay()) {
                sendSnapshot(player);
                return;
            }
            boolean success = request.isBuy()
                    ? buy(player, record, offers.getBuyOffer(request.getSlot()), request.getQuantity())
                    : sell(player, record, offers.getSellOffer(request.getSlot()), request.getQuantity());
            if (success) {
                data.markDirty();
                syncPlayerInventory(player);
                syncOpenMerchantViews(player.world, merchant.getMerchantId());
            } else {
                sendSnapshot(player);
            }
        } finally {
            container.endCommit();
        }
    }

    /**
     * Server-authoritative emerald/coin settlement.  The C2S packet carries
     * only the action, amount, and existing merchant-window session proof;
     * price, fee, balance, inventory capacity, and ownership are all read on
     * this scheduled logical-server path.
     */
    public static void handleEmeraldTransaction(EntityPlayerMP player, EmeraldTradeRequest request) {
        if (player == null || request == null || request.getAction() == null || player.world.isRemote
                || !(player.openContainer instanceof ContainerMerchantTrade)) {
            return;
        }
        ContainerMerchantTrade container = (ContainerMerchantTrade) player.openContainer;
        if (!container.getMerchantId().equals(request.getMerchantId()) || request.getWindowId() != container.windowId
                || !container.acceptRequestId(request.getRequestId()) || container.isCommitting()) {
            sendSnapshot(player);
            return;
        }
        container.beginCommit();
        try {
            if (request.getExpectedWorldDay() < 0L || request.getQuantity() <= 0
                    || request.getQuantity() > MAX_REQUEST_QUANTITY) {
                sendSnapshot(player);
                return;
            }
            Entity entity = player.world.getEntityByID(container.getEntityId());
            if (!(entity instanceof EntityMerchant) || entity.isDead || !container.canInteractWith(player)) {
                player.closeScreen();
                return;
            }
            EntityMerchant merchant = (EntityMerchant) entity;
            if (!merchant.getMerchantId().equals(request.getMerchantId()) || !ensureMerchantRecord(player.world, merchant)) {
                player.closeScreen();
                return;
            }
            PastoralWorldData data = PastoralWorldData.get(player.world);
            MerchantRecord record = data.getMerchantWorldState().getMerchant(merchant.getMerchantId());
            if (record == null || !record.isActive()) {
                player.closeScreen();
                return;
            }
            if (MerchantOfferService.ensureOffers(player.world, record)) {
                data.markDirty();
            }
            DailyOfferState offers = record.getDailyOfferState();
            if (offers == null || offers.getWorldDay() != request.getExpectedWorldDay()) {
                sendSnapshot(player);
                return;
            }
            boolean success = request.getAction() == EmeraldTradeAction.BUY
                    ? buyEmeralds(player, request.getQuantity())
                    : sellEmeralds(player, request.getQuantity());
            if (success) {
                data.markDirty();
                syncPlayerInventory(player);
                syncOpenMerchantViews(player.world, merchant.getMerchantId());
            } else {
                sendSnapshot(player);
            }
        } finally {
            container.endCommit();
        }
    }

    private static boolean ensureMerchantRecord(World world, EntityMerchant merchant) {
        if (merchant.getMerchantId() == null || merchant.getVillageId() == null || merchant.getStationId() == null) {
            return false;
        }
        MerchantRecord record = PastoralWorldData.get(world).getMerchantWorldState().getMerchant(merchant.getMerchantId());
        return record != null && merchant.getVillageId().equals(record.getVillageId())
                && merchant.getStationId().equals(record.getStationId());
    }

    private static boolean sell(EntityPlayerMP player, MerchantRecord record, DailyOffer offer, int quantity) {
        if (offer == null || !offer.isEnabled()) {
            return false;
        }
        TradeCatalogEntry entry = TradeCatalog.get(offer.getCatalogKey());
        if (entry == null || (entry.getPool() != TradePool.SELL_CORE && entry.getPool() != TradePool.SELL_SECONDARY)
                || entry.isEnchantment()) {
            return false;
        }
        long unitPrice;
        try {
            unitPrice = lisbam.pastoraleconomy.market.MarketService.getCurrentPrice(player.world, entry.getMarketKey());
        } catch (RuntimeException ignored) {
            return false;
        }
        long total = multiply(unitPrice, quantity);
        if (!canCredit(player, total)) {
            return false;
        }
        TradeVoucherStorageService.SaleInventory saleInventory =
                TradeVoucherStorageService.findSaleInventory(player);
        if (saleInventory.count(entry) < quantity) {
            return false;
        }
        TradeVoucherStorageService.Snapshot before = saleInventory.snapshot();
        if (saleInventory.remove(entry, quantity) != quantity) {
            saleInventory.restore(before);
            return false;
        }
        if (entry.getMarketKey().endsWith("sell/livestock/milk_bucket")
                && !saleInventory.insert(new ItemStack(Items.BUCKET, quantity, 0))) {
            saleInventory.restore(before);
            return false;
        }
        if (!CoinService.addCoins(player, total)) {
            saleInventory.restore(before);
            return false;
        }
        saleInventory.markDirty();
        return true;
    }

    private static boolean buy(EntityPlayerMP player, MerchantRecord record, DailyOffer offer, int quantity) {
        if (offer == null || !offer.isEnabled() || quantity <= 0) {
            return false;
        }
        TradeCatalogEntry entry = TradeCatalog.get(offer.getCatalogKey());
        if (entry == null || (entry.getPool() != TradePool.BUY_COMMON && entry.getPool() != TradePool.BUY_UNCOMMON
                && entry.getPool() != TradePool.BUY_RARE && entry.getPool() != TradePool.BUY_TREASURE)
                || !offer.canConsumeItems(quantity)) {
            return false;
        }
        String marketKey;
        try {
            marketKey = entry.getMarketKeyForLevel(offer.getEnchantmentLevel());
        } catch (RuntimeException ignored) {
            return false;
        }
        if (marketKey == null) {
            return false;
        }
        long unitPrice;
        try {
            unitPrice = lisbam.pastoraleconomy.market.MarketService.getCurrentPrice(player.world, marketKey);
        } catch (RuntimeException ignored) {
            return false;
        }
        long totalPrice = multiply(unitPrice, quantity);
        if (!canSpend(player, totalPrice)) {
            return false;
        }
        ItemStack output;
        try {
            output = entry.createStack(quantity, offer.getEnchantmentLevel());
        } catch (RuntimeException ignored) {
            return false;
        }
        if (!canFit(player.inventory, output)) {
            return false;
        }
        List<ItemStack> before = snapshotInventory(player.inventory);
        if (!CoinService.trySpend(player, totalPrice)) {
            return false;
        }
        if (!offer.consumeItems(quantity) || !insert(player.inventory, output)) {
            offer.restoreItems(quantity);
            restoreInventory(player.inventory, before);
            CoinService.addCoins(player, totalPrice);
            return false;
        }
        player.inventory.markDirty();
        return true;
    }

    private static boolean buyEmeralds(EntityPlayerMP player, int quantity) {
        long price = MarketService.getEmeraldCurrentPrice(player.world);
        long totalCost = EmeraldTradeRules.calculateBuyCost(price, quantity);
        if (totalCost < 0L || !canSpend(player, totalCost)
                || getEmeraldCapacity(player.inventory) < quantity) {
            return false;
        }
        List<ItemStack> before = snapshotInventory(player.inventory);
        if (!CoinService.trySpend(player, totalCost)) {
            return false;
        }
        if (!insert(player.inventory, new ItemStack(Items.EMERALD, quantity, 0))) {
            restoreInventory(player.inventory, before);
            CoinService.addCoins(player, totalCost);
            return false;
        }
        player.inventory.markDirty();
        return true;
    }

    private static boolean sellEmeralds(EntityPlayerMP player, int quantity) {
        if (countEmeralds(player.inventory) < quantity) {
            return false;
        }
        long income = EmeraldTradeRules.calculateSellIncome(MarketService.getEmeraldCurrentPrice(player.world), quantity);
        if (income < 0L || !canCredit(player, income)) {
            return false;
        }
        List<ItemStack> before = snapshotInventory(player.inventory);
        if (removeEmeralds(player.inventory, quantity) != quantity) {
            restoreInventory(player.inventory, before);
            return false;
        }
        if (!CoinService.addCoins(player, income)) {
            restoreInventory(player.inventory, before);
            return false;
        }
        player.inventory.markDirty();
        return true;
    }

    private static long multiply(long left, long right) {
        if (left <= 0L || right <= 0L || left > Long.MAX_VALUE / right) {
            return -1L;
        }
        return left * right;
    }

    private static boolean canCredit(EntityPlayerMP player, long amount) {
        return amount >= 0L && CoinService.getBalance(player) <= Long.MAX_VALUE - amount;
    }

    private static boolean canSpend(EntityPlayerMP player, long amount) {
        return amount >= 0L && CoinService.canAfford(player, amount);
    }

    private static List<ItemStack> snapshotInventory(InventoryPlayer inventory) {
        List<ItemStack> result = new ArrayList<ItemStack>(inventory.mainInventory.size());
        for (ItemStack stack : inventory.mainInventory) {
            result.add(stack == null ? ItemStack.EMPTY : stack.copy());
        }
        return result;
    }

    private static void restoreInventory(InventoryPlayer inventory, List<ItemStack> snapshot) {
        for (int index = 0; index < snapshot.size(); index++) {
            inventory.setInventorySlotContents(index, snapshot.get(index).copy());
        }
        inventory.markDirty();
    }

    private static boolean canFit(InventoryPlayer inventory, ItemStack output) {
        if (output == null || output.isEmpty()) {
            return true;
        }
        int remaining = output.getCount();
        int maxStack = Math.min(output.getMaxStackSize(), inventory.getInventoryStackLimit());
        for (ItemStack existing : inventory.mainInventory) {
            if (existing != null && !existing.isEmpty() && existing.isItemEqual(output)
                    && ItemStack.areItemStackTagsEqual(existing, output)) {
                remaining -= Math.max(0, maxStack - existing.getCount());
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        for (ItemStack existing : inventory.mainInventory) {
            if (existing == null || existing.isEmpty()) {
                remaining -= maxStack;
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean insert(InventoryPlayer inventory, ItemStack output) {
        ItemStack remaining = output.copy();
        int maxStack = Math.min(remaining.getMaxStackSize(), inventory.getInventoryStackLimit());
        for (int index = 0; index < inventory.mainInventory.size() && !remaining.isEmpty(); index++) {
            ItemStack existing = inventory.mainInventory.get(index);
            if (existing == null || existing.isEmpty() || !existing.isItemEqual(remaining)
                    || !ItemStack.areItemStackTagsEqual(existing, remaining)) {
                continue;
            }
            int room = Math.min(maxStack, existing.getMaxStackSize()) - existing.getCount();
            if (room > 0) {
                int moved = Math.min(room, remaining.getCount());
                existing.grow(moved);
                remaining.shrink(moved);
            }
        }
        for (int index = 0; index < inventory.mainInventory.size() && !remaining.isEmpty(); index++) {
            ItemStack existing = inventory.mainInventory.get(index);
            if (existing == null || existing.isEmpty()) {
                int moved = Math.min(maxStack, remaining.getCount());
                ItemStack placed = remaining.copy();
                placed.setCount(moved);
                inventory.setInventorySlotContents(index, placed);
                remaining.shrink(moved);
            }
        }
        return remaining.isEmpty();
    }

    private static int countEmeralds(InventoryPlayer inventory) {
        int total = 0;
        for (ItemStack stack : inventory.mainInventory) {
            if (stack != null && !stack.isEmpty() && stack.getItem() == Items.EMERALD) {
                total = addAvailableCounts(total, stack.getCount());
            }
        }
        return total;
    }

    /** Number of additional blank emerald items the actual player inventory can accept. */
    private static int getEmeraldCapacity(InventoryPlayer inventory) {
        ItemStack emerald = new ItemStack(Items.EMERALD, 1, 0);
        int maxStack = Math.min(emerald.getMaxStackSize(), inventory.getInventoryStackLimit());
        int capacity = 0;
        for (ItemStack existing : inventory.mainInventory) {
            if (existing != null && !existing.isEmpty() && existing.isItemEqual(emerald)
                    && ItemStack.areItemStackTagsEqual(existing, emerald)) {
                capacity = addAvailableCounts(capacity, Math.max(0, maxStack - existing.getCount()));
            } else if (existing == null || existing.isEmpty()) {
                capacity = addAvailableCounts(capacity, maxStack);
            }
        }
        return capacity;
    }

    private static int removeEmeralds(InventoryPlayer inventory, int amount) {
        int remaining = amount;
        for (int index = 0; index < inventory.mainInventory.size() && remaining > 0; index++) {
            ItemStack stack = inventory.mainInventory.get(index);
            if (stack == null || stack.isEmpty() || stack.getItem() != Items.EMERALD) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            if (stack.isEmpty()) {
                inventory.setInventorySlotContents(index, ItemStack.EMPTY);
            }
            remaining -= removed;
        }
        return amount - remaining;
    }

    private static int addAvailableCounts(int left, int right) {
        return left > Integer.MAX_VALUE - right ? Integer.MAX_VALUE : left + right;
    }

    private static void sendSnapshot(EntityPlayerMP player) {
        if (player.openContainer instanceof ContainerMerchantTrade) {
            MerchantTradeSnapshot snapshot = createSnapshot(player, (ContainerMerchantTrade) player.openContainer);
            if (snapshot != null) {
                ModNetwork.CHANNEL.sendTo(new SyncMerchantTradeMessage(snapshot), player);
            }
        }
    }

    /**
     * The merchant window deliberately has no inventory slots. Send window 0
     * explicitly so its server-side inventory mutation reaches the client in
     * the same transaction tick instead of waiting for the screen to close.
     */
    private static void syncPlayerInventory(EntityPlayerMP player) {
        player.sendContainerToPlayer(player.inventoryContainer);
    }

    private static void syncOpenMerchantViews(World world, UUID merchantId) {
        if (!(world instanceof net.minecraft.world.WorldServer)) {
            return;
        }
        net.minecraft.server.MinecraftServer server = ((net.minecraft.world.WorldServer) world).getMinecraftServer();
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (player.openContainer instanceof ContainerMerchantTrade
                    && merchantId.equals(((ContainerMerchantTrade) player.openContainer).getMerchantId())) {
                sendSnapshot(player);
            }
        }
    }

    private static MerchantTradeSnapshot createSnapshot(EntityPlayerMP player, ContainerMerchantTrade container) {
        Entity entity = player.world.getEntityByID(container.getEntityId());
        if (!(entity instanceof EntityMerchant) || entity.isDead || !container.canInteractWith(player)
                || !ensureMerchantRecord(player.world, (EntityMerchant) entity)) {
            return null;
        }
        MerchantRecord record = PastoralWorldData.get(player.world).getMerchantWorldState().getMerchant(container.getMerchantId());
        if (record == null || !record.isActive()) {
            return null;
        }
        if (MerchantOfferService.ensureOffers(player.world, record)) {
            PastoralWorldData.get(player.world).markDirty();
        }
        DailyOfferState state = record.getDailyOfferState();
        if (state == null) {
            return null;
        }
        MarketPriceSnapshot prices = MarketService.getPriceSnapshot(player.world);
        TradeVoucherStorageService.SaleInventory saleInventory =
                TradeVoucherStorageService.findSaleInventory(player);
        long emeraldCurrentPrice = MarketService.getEmeraldCurrentPrice(player.world);
        long emeraldPreviousPrice = MarketService.getEmeraldPreviousPrice(player.world);
        int emeraldHoldings = countEmeralds(player.inventory);
        int emeraldMaxBuy = Math.min(MAX_REQUEST_QUANTITY, Math.min(
                EmeraldTradeRules.getMaximumAffordableAmount(CoinService.getBalance(player), emeraldCurrentPrice,
                        MAX_REQUEST_QUANTITY),
                getEmeraldCapacity(player.inventory)
        ));
        int emeraldMaxSell = Math.min(MAX_REQUEST_QUANTITY, emeraldHoldings);
        return new MerchantTradeSnapshot(record.getMerchantId(), container.windowId, state.getWorldDay(),
                CoinService.getBalance(player), emeraldCurrentPrice, emeraldPreviousPrice, emeraldHoldings,
                emeraldMaxBuy, emeraldMaxSell, createViews(state.getSellOffers(), prices, saleInventory),
                createViews(state.getBuyOffers(), prices, null));
    }

    private static List<MerchantTradeOfferView> createViews(List<DailyOffer> offers, MarketPriceSnapshot prices,
                                                            TradeVoucherStorageService.SaleInventory saleInventory) {
        List<MerchantTradeOfferView> result = new ArrayList<MerchantTradeOfferView>(offers.size());
        for (DailyOffer offer : offers) {
            TradeCatalogEntry entry = offer.isEnabled() ? TradeCatalog.get(offer.getCatalogKey()) : null;
            if (entry == null) {
                result.add(new MerchantTradeOfferView(false, "", 0, 0L, null, TradeCatalogEntry.UNLIMITED_STOCK));
                continue;
            }
            long price = 0L;
            Long previous = null;
            try {
                String marketKey = entry.getMarketKeyForLevel(offer.getEnchantmentLevel());
                if (marketKey == null) {
                    throw new IllegalStateException("Invalid resolved offer level.");
                }
                Long current = prices.getCurrentPrice(marketKey);
                if (current == null) {
                    throw new IllegalStateException("Current market snapshot is incomplete: " + marketKey);
                }
                price = current.longValue();
                previous = prices.getPreviousPrice(marketKey);
            } catch (RuntimeException ignored) {
                // A malformed/legacy catalog entry is displayed disabled and never tradable.
                result.add(new MerchantTradeOfferView(false, "", 0, 0L, null, TradeCatalogEntry.UNLIMITED_STOCK));
                continue;
            }
            int remaining = saleInventory == null ? offer.getRemainingItems() : saleInventory.countLinkedChests(entry);
            result.add(new MerchantTradeOfferView(true, entry.getCatalogKey(), entry.getItemStackLimit(), price, previous,
                    remaining, offer.getEnchantmentLevel()));
        }
        return result;
    }

    /** Packet-safe transaction request value object. */
    public static final class MerchantTradeRequest {
        private final UUID merchantId;
        private final int windowId;
        private final long expectedWorldDay;
        private final boolean buy;
        private final int slot;
        private final int quantity;
        private final int requestId;

        public MerchantTradeRequest(UUID merchantId, int windowId, long expectedWorldDay, boolean buy, int slot,
                                    int quantity, int requestId) {
            this.merchantId = merchantId;
            this.windowId = windowId;
            this.expectedWorldDay = expectedWorldDay;
            this.buy = buy;
            this.slot = slot;
            this.quantity = quantity;
            this.requestId = requestId;
        }
        public UUID getMerchantId() { return merchantId; }
        public int getWindowId() { return windowId; }
        public long getExpectedWorldDay() { return expectedWorldDay; }
        public boolean isBuy() { return buy; }
        public int getSlot() { return slot; }
        public int getQuantity() { return quantity; }
        public int getRequestId() { return requestId; }
    }

    /** Packet-safe emerald spot-market request value object. */
    public static final class EmeraldTradeRequest {
        private final UUID merchantId;
        private final int windowId;
        private final long expectedWorldDay;
        private final EmeraldTradeAction action;
        private final int quantity;
        private final int requestId;

        public EmeraldTradeRequest(UUID merchantId, int windowId, long expectedWorldDay, EmeraldTradeAction action,
                                   int quantity, int requestId) {
            this.merchantId = merchantId;
            this.windowId = windowId;
            this.expectedWorldDay = expectedWorldDay;
            this.action = action;
            this.quantity = quantity;
            this.requestId = requestId;
        }

        public UUID getMerchantId() { return merchantId; }
        public int getWindowId() { return windowId; }
        public long getExpectedWorldDay() { return expectedWorldDay; }
        public EmeraldTradeAction getAction() { return action; }
        public int getQuantity() { return quantity; }
        public int getRequestId() { return requestId; }
    }
}
