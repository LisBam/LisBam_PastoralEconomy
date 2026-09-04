package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.data.player.CoinService;
import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import lisbam.pastoraleconomy.entity.EntityMerchant;
import lisbam.pastoraleconomy.gui.ContainerMerchantTrade;
import lisbam.pastoraleconomy.gui.GuiIds;
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
    private static final int MAX_REQUEST_QUANTITY = 4096;

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
        List<ItemStack> before = snapshotInventory(player.inventory);
        int held = countHeld(player.inventory, entry);
        if (held < quantity) {
            return false;
        }
        if (entry.getMarketKey().endsWith("sell/livestock/milk_bucket")) {
            // Milk buckets are non-stackable: removing the sold buckets first
            // creates the exact slots that must receive the returned buckets.
            List<ItemStack> simulated = snapshotInventory(player.inventory);
            if (removeHeld(player.inventory, entry, quantity) != quantity) {
                restoreInventory(player.inventory, before);
                return false;
            }
            boolean bucketCapacity = canFit(player.inventory, new ItemStack(Items.BUCKET, quantity, 0));
            restoreInventory(player.inventory, simulated);
            if (!bucketCapacity) {
                return false;
            }
        }
        if (removeHeld(player.inventory, entry, quantity) != quantity) {
            restoreInventory(player.inventory, before);
            return false;
        }
        if (entry.getMarketKey().endsWith("sell/livestock/milk_bucket")
                && !insert(player.inventory, new ItemStack(Items.BUCKET, quantity, 0))) {
            restoreInventory(player.inventory, before);
            return false;
        }
        if (!CoinService.addCoins(player, total)) {
            restoreInventory(player.inventory, before);
            return false;
        }
        player.inventory.markDirty();
        return true;
    }

    private static boolean buy(EntityPlayerMP player, MerchantRecord record, DailyOffer offer, int bundles) {
        if (offer == null || !offer.isEnabled() || bundles <= 0) {
            return false;
        }
        TradeCatalogEntry entry = TradeCatalog.get(offer.getCatalogKey());
        if (entry == null || (entry.getPool() != TradePool.BUY_COMMON && entry.getPool() != TradePool.BUY_UNCOMMON
                && entry.getPool() != TradePool.BUY_RARE && entry.getPool() != TradePool.BUY_TREASURE)
                || !offer.canConsume(bundles)) {
            return false;
        }
        long itemCountLong = (long) entry.getBundleSize() * (long) bundles;
        if (itemCountLong <= 0L || itemCountLong > Integer.MAX_VALUE) {
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
        long bundlePrice;
        try {
            bundlePrice = lisbam.pastoraleconomy.market.MarketService.getCurrentPrice(player.world, marketKey);
        } catch (RuntimeException ignored) {
            return false;
        }
        long totalPrice = multiply(bundlePrice, bundles);
        if (!canSpend(player, totalPrice)) {
            return false;
        }
        int itemCount = (int) itemCountLong;
        ItemStack output;
        try {
            output = entry.createStack(itemCount, offer.getEnchantmentLevel());
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
        if (!offer.consume(bundles) || !insert(player.inventory, output)) {
            offer.restore(bundles);
            restoreInventory(player.inventory, before);
            CoinService.addCoins(player, totalPrice);
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

    private static int countHeld(InventoryPlayer inventory, TradeCatalogEntry entry) {
        int total = 0;
        for (ItemStack stack : inventory.mainInventory) {
            if (entry.matches(stack)) {
                if (total > Integer.MAX_VALUE - stack.getCount()) {
                    return Integer.MAX_VALUE;
                }
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int removeHeld(InventoryPlayer inventory, TradeCatalogEntry entry, int quantity) {
        int remaining = quantity;
        for (int index = 0; index < inventory.mainInventory.size() && remaining > 0; index++) {
            ItemStack stack = inventory.mainInventory.get(index);
            if (!entry.matches(stack)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
            if (stack.isEmpty()) {
                inventory.setInventorySlotContents(index, ItemStack.EMPTY);
            }
        }
        return quantity - remaining;
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

    private static void sendSnapshot(EntityPlayerMP player) {
        if (player.openContainer instanceof ContainerMerchantTrade) {
            MerchantTradeSnapshot snapshot = createSnapshot(player, (ContainerMerchantTrade) player.openContainer);
            if (snapshot != null) {
                ModNetwork.CHANNEL.sendTo(new SyncMerchantTradeMessage(snapshot), player);
            }
        }
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
        return new MerchantTradeSnapshot(record.getMerchantId(), container.windowId, state.getWorldDay(),
                CoinService.getBalance(player), createViews(player.world, state.getSellOffers()), createViews(player.world, state.getBuyOffers()));
    }

    private static List<MerchantTradeOfferView> createViews(World world, List<DailyOffer> offers) {
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
                price = lisbam.pastoraleconomy.market.MarketService.getCurrentPrice(world, marketKey);
                previous = lisbam.pastoraleconomy.market.MarketService.getPreviousPrice(world, marketKey);
            } catch (RuntimeException ignored) {
                // A malformed/legacy catalog entry is displayed disabled and never tradable.
                result.add(new MerchantTradeOfferView(false, "", 0, 0L, null, TradeCatalogEntry.UNLIMITED_STOCK));
                continue;
            }
            result.add(new MerchantTradeOfferView(true, entry.getCatalogKey(), entry.getBundleSize(), price, previous,
                    offer.getRemainingBundles(), offer.getEnchantmentLevel()));
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
}
