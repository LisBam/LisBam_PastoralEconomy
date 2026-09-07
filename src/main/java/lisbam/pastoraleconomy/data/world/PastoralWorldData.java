package lisbam.pastoraleconomy.data.world;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.config.ModSettings;
import lisbam.pastoraleconomy.market.MarketCatalog;
import lisbam.pastoraleconomy.market.MarketCommodity;
import lisbam.pastoraleconomy.market.EmeraldMarketPriceGenerator;
import lisbam.pastoraleconomy.market.MarketHistoryPoint;
import lisbam.pastoraleconomy.market.MarketPriceSnapshot;
import lisbam.pastoraleconomy.market.MarketPriceGenerator;
import lisbam.pastoraleconomy.merchant.MerchantWorldState;
import lisbam.pastoraleconomy.merchant.VoucherChestRegistry;
import lisbam.pastoraleconomy.shipping.ShippingBoxPayoutState;
import lisbam.pastoraleconomy.transport.TransportWorldState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * One world-shared root for this save. All future world economy data accesses
 * must use get(World), which resolves to the server overworld MapStorage.
 */
public final class PastoralWorldData extends WorldSavedData {
    public static final String DATA_NAME = LisBamPastoralEconomy.MODID + "_world_data";
    public static final int DATA_VERSION = 11;
    public static final long MARKET_DAY_TICKS = 24000L;
    /** The market book has one 30-day window, so older points must never grow the save. */
    public static final int MARKET_HISTORY_RETENTION_DAYS = 30;
    private static final String KEY_DATA_VERSION = "dataVersion";
    private static final String KEY_INITIALIZED = "firstInitializationCompleted";
    private static final String KEY_MARKET = "market";
    private static final String KEY_MERCHANT = "merchant";
    private static final String KEY_TRANSPORT = "transport";
    private static final String KEY_VOUCHER_CHESTS = "voucherChests";
    private static final String KEY_SHIPPING_BOX_PAYOUTS = "shippingBoxPayouts";
    private static final String KEY_MARKET_INITIALIZED = "initialized";
    private static final String KEY_MARKET_SEED = "seed";
    private static final String KEY_FIRST_MARKET_DAY = "firstDay";
    private static final String KEY_DAY = "day";
    private static final String KEY_CURRENT_SNAPSHOT = "currentSnapshot";
    private static final String KEY_PREVIOUS_SNAPSHOT = "previousSnapshot";
    private static final String KEY_SNAPSHOT_PRICES = "prices";
    private static final String KEY_PRICE_KEY = "key";
    private static final String KEY_PRICE_VALUE = "price";
    private static final String KEY_CROP_HISTORIES = "cropHistories";
    private static final String KEY_HISTORY_KEY = "key";
    private static final String KEY_HISTORY_POINTS = "points";
    private static final String KEY_EMERALD_MARKET_INITIALIZED = "emeraldInitialized";
    private static final String KEY_EMERALD_CURRENT_PRICE = "emeraldCurrentPrice";
    private static final String KEY_EMERALD_PREVIOUS_PRICE = "emeraldPreviousPrice";
    private static final String KEY_EMERALD_LAST_UPDATE_DAY = "emeraldLastUpdateDay";
    private static final String KEY_EMERALD_HISTORY = "emeraldHistory";

    private int dataVersion = DATA_VERSION;
    private boolean firstInitializationCompleted;
    private boolean marketInitialized;
    private long marketSeed;
    private long firstMarketDay = -1L;
    private long currentMarketDay = -1L;
    private long previousMarketDay = -1L;
    private final Map<String, Long> currentMarketPrices = new LinkedHashMap<String, Long>();
    private final Map<String, Long> previousMarketPrices = new LinkedHashMap<String, Long>();
    private final Map<String, NavigableMap<Long, Long>> cropPriceHistory =
            new LinkedHashMap<String, NavigableMap<Long, Long>>();
    /** Independent high-volatility price persisted beside the normal market snapshots. */
    private boolean emeraldMarketInitialized;
    private long emeraldCurrentPrice = EmeraldMarketPriceGenerator.INITIAL_PRICE;
    private long emeraldPreviousPrice = EmeraldMarketPriceGenerator.INITIAL_PRICE;
    private long emeraldLastUpdateDay = -1L;
    /** The market book retains only the actual daily emerald prices needed for its 30-day chart. */
    private final NavigableMap<Long, Long> emeraldPriceHistory = new TreeMap<Long, Long>();
    /** Batch 08--10 world-owned village, station, merchant, offer, and stock state. */
    private final MerchantWorldState merchantWorldState = new MerchantWorldState();
    /** Batch 14 world-owned physical transport-node registry. */
    private final TransportWorldState transportWorldState = new TransportWorldState();
    /** Physical chest positions retained solely while they hold a bound trade voucher. */
    private final VoucherChestRegistry voucherChestRegistry = new VoucherChestRegistry();
    /** Daily batch marker and deferred owner payments for shipping boxes. */
    private final ShippingBoxPayoutState shippingBoxPayoutState = new ShippingBoxPayoutState();

    public PastoralWorldData() {
        this(DATA_NAME);
    }

    public PastoralWorldData(String name) {
        super(name);
    }

    public static PastoralWorldData get(World world) {
        WorldServer overworld = getOverworld(world);
        MapStorage storage = overworld.getMapStorage();
        PastoralWorldData data = (PastoralWorldData) storage.getOrLoadData(PastoralWorldData.class, DATA_NAME);
        if (data == null) {
            data = new PastoralWorldData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    /** Minecraft day, deliberately based on the shared overworld's day/night time rather than total ticks. */
    public static long getAuthoritativeMarketDay(World world) {
        return getOverworld(world).getWorldTime() / MARKET_DAY_TICKS;
    }

    public static long getAuthoritativeWorldSeed(World world) {
        return getOverworld(world).getSeed();
    }

    private static WorldServer getOverworld(World world) {
        if (world.isRemote || !(world instanceof WorldServer)) {
            throw new IllegalStateException("Pastoral world data is server-authoritative.");
        }

        MinecraftServer server = ((WorldServer) world).getMinecraftServer();
        WorldServer overworld = server.getWorld(0);
        if (overworld == null) {
            throw new IllegalStateException("The server overworld is unavailable.");
        }
        return overworld;
    }

    public boolean completeFirstInitialization(WorldServer overworld) {
        if (firstInitializationCompleted) {
            return false;
        }
        overworld.getGameRules().setOrCreateGameRule("keepInventory", "true");
        firstInitializationCompleted = true;
        markDirty();
        return true;
    }

    public boolean isFirstInitializationCompleted() {
        return firstInitializationCompleted;
    }

    /**
     * Server-side market transition. The default path advances every skipped
     * day because every price is deliberately chained to its saved predecessor.
     * Existing-day snapshots are never regenerated when a setting or catalog
     * base price changes; a new base applies only on a later daily step.
     */
    public synchronized void ensureMarketDay(long worldDay, long authoritativeWorldSeed) {
        if (!marketInitialized) {
            marketInitialized = true;
            marketSeed = MarketPriceGenerator.createMarketSeed(authoritativeWorldSeed);
            firstMarketDay = worldDay;
            setInitialSnapshot(worldDay, Collections.<String, Long>emptyMap());
            previousMarketDay = -1L;
            previousMarketPrices.clear();
            appendCropHistory(worldDay, currentMarketPrices);
            ensureEmeraldMarketDay(worldDay);
            markDirty();
            return;
        }

        if (worldDay == currentMarketDay) {
            boolean changed = fillMissingCurrentPrices();
            if (ensureCurrentCropHistory()) {
                changed = true;
            }
            if (ensureEmeraldMarketDay(worldDay)) {
                changed = true;
            }
            if (changed) {
                markDirty();
            }
            return;
        }

        if (worldDay > currentMarketDay) {
            advanceMarketDays(worldDay, ModSettings.isMoreStableMarketVolatility());
        } else {
            // Rollbacks are administrator-driven time changes. Retained crop
            // prices are restored exactly; non-book prices use the legacy
            // deterministic fallback when no full historic snapshot exists.
            restoreMarketDay(worldDay);
        }
        ensureEmeraldMarketDay(worldDay);
        markDirty();
    }

    public synchronized boolean isMarketInitialized() {
        return marketInitialized;
    }

    public synchronized long getCurrentMarketDay() {
        return currentMarketDay;
    }

    public synchronized Long getCurrentMarketPrice(String key) {
        return currentMarketPrices.get(key);
    }

    public synchronized Long getPreviousMarketPrice(String key) {
        return previousMarketDay < 0L ? null : previousMarketPrices.get(key);
    }

    public synchronized boolean hasPreviousMarketSnapshot() {
        return previousMarketDay >= 0L;
    }

    public synchronized long getEmeraldCurrentPrice() {
        return emeraldCurrentPrice;
    }

    public synchronized long getEmeraldPreviousPrice() {
        return emeraldPreviousPrice;
    }

    public synchronized long getEmeraldLastUpdateDay() {
        return emeraldLastUpdateDay;
    }

    public synchronized List<MarketHistoryPoint> getEmeraldHistory(int limit, Long beforeExclusiveDay) {
        return getPriceHistory(emeraldPriceHistory, emeraldLastUpdateDay, limit, beforeExclusiveDay);
    }

    /** One immutable price view avoids repeated WorldSavedData lookups per merchant GUI snapshot. */
    public synchronized MarketPriceSnapshot createMarketPriceSnapshot() {
        return new MarketPriceSnapshot(currentMarketDay,
                new LinkedHashMap<String, Long>(currentMarketPrices),
                new LinkedHashMap<String, Long>(previousMarketPrices));
    }

    public synchronized List<MarketHistoryPoint> getCropHistory(String key, long visibleThroughDay, int limit,
                                                                 Long beforeExclusiveDay) {
        return getPriceHistory(cropPriceHistory.get(key), visibleThroughDay, limit, beforeExclusiveDay);
    }

    public synchronized MarketHistoryPoint getCropHistoryPoint(String key, long worldDay, long visibleThroughDay) {
        return getPriceHistoryPoint(cropPriceHistory.get(key), worldDay, visibleThroughDay);
    }

    /**
     * The returned state is mutable only on the logical server's main thread.
     * Callers must mark this WorldSavedData dirty after a real mutation.
     */
    public synchronized MerchantWorldState getMerchantWorldState() {
        return merchantWorldState;
    }

    /**
     * The returned registry is mutable only on the logical server's main
     * thread. Its caller must mark this WorldSavedData dirty after mutation.
     */
    public synchronized TransportWorldState getTransportWorldState() {
        return transportWorldState;
    }

    /** Mutable on the logical server main thread; callers mark this root dirty after a mutation. */
    public synchronized VoucherChestRegistry getVoucherChestRegistry() {
        return voucherChestRegistry;
    }

    /** Mutable on the logical server main thread; callers mark this root dirty after mutation. */
    public synchronized ShippingBoxPayoutState getShippingBoxPayoutState() {
        return shippingBoxPayoutState;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        int storedVersion = compound.hasKey(KEY_DATA_VERSION) ? compound.getInteger(KEY_DATA_VERSION) : 1;
        firstInitializationCompleted = compound.getBoolean(KEY_INITIALIZED);
        clearMarketData();
        merchantWorldState.readFromNBT(new NBTTagCompound());
        transportWorldState.readFromNBT(new NBTTagCompound());
        voucherChestRegistry.readFromNBT(new NBTTagCompound());
        shippingBoxPayoutState.readFromNBT(new NBTTagCompound());

        if (storedVersion >= 3 && compound.hasKey(KEY_MARKET, 10)) {
            readMarket(compound.getCompoundTag(KEY_MARKET));
        }
        if (storedVersion >= 4 && compound.hasKey(KEY_MERCHANT, 10)) {
            merchantWorldState.readFromNBT(compound.getCompoundTag(KEY_MERCHANT));
        }
        if (storedVersion >= 6 && compound.hasKey(KEY_TRANSPORT, 10)) {
            transportWorldState.readFromNBT(compound.getCompoundTag(KEY_TRANSPORT));
        }
        if (storedVersion >= 8 && compound.hasKey(KEY_VOUCHER_CHESTS, 10)) {
            voucherChestRegistry.readFromNBT(compound.getCompoundTag(KEY_VOUCHER_CHESTS));
        }
        if (storedVersion >= 10 && compound.hasKey(KEY_SHIPPING_BOX_PAYOUTS, 10)) {
            shippingBoxPayoutState.readFromNBT(compound.getCompoundTag(KEY_SHIPPING_BOX_PAYOUTS));
        }
        dataVersion = DATA_VERSION;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        dataVersion = DATA_VERSION;
        compound.setInteger(KEY_DATA_VERSION, dataVersion);
        compound.setBoolean(KEY_INITIALIZED, firstInitializationCompleted);
        compound.setTag(KEY_MARKET, writeMarket());
        compound.setTag(KEY_MERCHANT, merchantWorldState.writeToNBT());
        compound.setTag(KEY_TRANSPORT, transportWorldState.writeToNBT());
        compound.setTag(KEY_VOUCHER_CHESTS, voucherChestRegistry.writeToNBT());
        compound.setTag(KEY_SHIPPING_BOX_PAYOUTS, shippingBoxPayoutState.writeToNBT());
        return compound;
    }

    private void setInitialSnapshot(long day, Map<String, Long> existingPrices) {
        currentMarketDay = day;
        currentMarketPrices.clear();
        currentMarketPrices.putAll(createInitialSnapshot(day, existingPrices));
    }

    private boolean fillMissingCurrentPrices() {
        boolean changed = false;
        for (MarketCommodity commodity : MarketCatalog.getAll()) {
            if (!currentMarketPrices.containsKey(commodity.getKey())) {
                Long previous = previousMarketPrices.get(commodity.getKey());
                long recovered = previous == null
                        ? MarketPriceGenerator.calculateInitialPrice(marketSeed, currentMarketDay, commodity,
                        ModSettings.isMoreStableMarketVolatility())
                        : MarketPriceGenerator.calculateNextPrice(marketSeed, currentMarketDay, commodity,
                        previous.longValue(), ModSettings.isMoreStableMarketVolatility());
                currentMarketPrices.put(commodity.getKey(), recovered);
                changed = true;
            }
        }
        return changed;
    }

    private Map<String, Long> createInitialSnapshot(long day, Map<String, Long> existingPrices) {
        Map<String, Long> snapshot = new LinkedHashMap<String, Long>(existingPrices);
        for (MarketCommodity commodity : MarketCatalog.getAll()) {
            if (!snapshot.containsKey(commodity.getKey())) {
                snapshot.put(commodity.getKey(), MarketPriceGenerator.calculateInitialPrice(marketSeed, day, commodity,
                        ModSettings.isMoreStableMarketVolatility()));
            }
        }
        return snapshot;
    }

    private void advanceMarketDays(long targetDay, boolean moreStableVolatility) {
        long day = currentMarketDay;
        while (day < targetDay) {
            if (day == Long.MAX_VALUE) {
                break;
            }
            day++;
            Map<String, Long> oldCurrent = new LinkedHashMap<String, Long>(currentMarketPrices);
            Map<String, Long> nextCurrent = new LinkedHashMap<String, Long>();
            for (MarketCommodity commodity : MarketCatalog.getAll()) {
                Long previous = oldCurrent.get(commodity.getKey());
                if (previous == null || previous.longValue() <= 0L) {
                    previous = Long.valueOf(MarketPriceGenerator.calculateInitialPrice(marketSeed, day - 1L, commodity,
                            moreStableVolatility));
                }
                nextCurrent.put(commodity.getKey(), Long.valueOf(MarketPriceGenerator.calculateNextPrice(
                        marketSeed, day, commodity, previous.longValue(), moreStableVolatility)));
            }
            previousMarketDay = day - 1L;
            previousMarketPrices.clear();
            previousMarketPrices.putAll(oldCurrent);
            currentMarketDay = day;
            currentMarketPrices.clear();
            currentMarketPrices.putAll(nextCurrent);
            appendCropHistory(day, nextCurrent);
        }
    }

    private void restoreMarketDay(long worldDay) {
        currentMarketDay = worldDay;
        currentMarketPrices.clear();
        currentMarketPrices.putAll(createRestoredSnapshot(worldDay));
        previousMarketPrices.clear();
        if (worldDay > firstMarketDay) {
            previousMarketDay = worldDay - 1L;
            previousMarketPrices.putAll(createRestoredSnapshot(previousMarketDay));
        } else {
            previousMarketDay = -1L;
        }
    }

    private Map<String, Long> createRestoredSnapshot(long day) {
        Map<String, Long> snapshot = new LinkedHashMap<String, Long>();
        for (MarketCommodity commodity : MarketCatalog.getAll()) {
            Long retained = getRetainedCropPrice(commodity.getKey(), day);
            snapshot.put(commodity.getKey(), retained == null
                    ? Long.valueOf(MarketPriceGenerator.calculatePrice(marketSeed, day, commodity)) : retained);
        }
        return snapshot;
    }

    private Long getRetainedCropPrice(String key, long day) {
        NavigableMap<Long, Long> history = cropPriceHistory.get(key);
        return history == null ? null : history.get(Long.valueOf(day));
    }

    private boolean ensureCurrentCropHistory() {
        boolean changed = false;
        for (MarketCommodity commodity : MarketCatalog.getHistoryTracked()) {
            NavigableMap<Long, Long> history = cropPriceHistory.get(commodity.getKey());
            if (history == null) {
                history = new TreeMap<Long, Long>();
                cropPriceHistory.put(commodity.getKey(), history);
            }
            if (!history.containsKey(Long.valueOf(currentMarketDay))) {
                Long price = currentMarketPrices.get(commodity.getKey());
                if (price != null) {
                    history.put(Long.valueOf(currentMarketDay), price);
                    trimCropHistory(history);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private void appendCropHistory(long day, Map<String, Long> prices) {
        for (MarketCommodity commodity : MarketCatalog.getHistoryTracked()) {
            NavigableMap<Long, Long> history = cropPriceHistory.get(commodity.getKey());
            if (history == null) {
                history = new TreeMap<Long, Long>();
                cropPriceHistory.put(commodity.getKey(), history);
            }
            Long price = prices.get(commodity.getKey());
            if (price != null && price.longValue() > 0L) {
                history.put(Long.valueOf(day), price);
                trimCropHistory(history);
            }
        }
    }

    private void trimCropHistory(NavigableMap<Long, Long> history) {
        while (history.size() > MARKET_HISTORY_RETENTION_DAYS) {
            history.pollFirstEntry();
        }
    }

    private List<MarketHistoryPoint> getPriceHistory(NavigableMap<Long, Long> history, long visibleThroughDay,
                                                      int limit, Long beforeExclusiveDay) {
        if (limit <= 0 || history == null || history.isEmpty() || visibleThroughDay < 0L) {
            return Collections.emptyList();
        }
        NavigableMap<Long, Long> visible = history.headMap(visibleThroughDay, true);
        if (beforeExclusiveDay != null && !visible.isEmpty()) {
            long effectiveBefore = beforeExclusiveDay.longValue();
            if (effectiveBefore <= visible.firstKey().longValue()) {
                return Collections.emptyList();
            }
            if (effectiveBefore <= visible.lastKey().longValue()) {
                visible = visible.headMap(effectiveBefore, false);
            }
        }
        if (visible.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> days = new ArrayList<Long>();
        Iterator<Long> descending = visible.descendingKeySet().iterator();
        int effectiveLimit = Math.min(limit, MARKET_HISTORY_RETENTION_DAYS);
        while (descending.hasNext() && days.size() < effectiveLimit) {
            days.add(descending.next());
        }
        Collections.reverse(days);
        List<MarketHistoryPoint> points = new ArrayList<MarketHistoryPoint>();
        for (Long day : days) {
            Map.Entry<Long, Long> previous = history.lowerEntry(day);
            points.add(new MarketHistoryPoint(day.longValue(), history.get(day).longValue(),
                    previous == null ? null : previous.getValue()));
        }
        return Collections.unmodifiableList(points);
    }

    private MarketHistoryPoint getPriceHistoryPoint(NavigableMap<Long, Long> history, long worldDay,
                                                     long visibleThroughDay) {
        if (worldDay > visibleThroughDay || history == null || !history.containsKey(worldDay)) {
            return null;
        }
        Map.Entry<Long, Long> previous = history.lowerEntry(worldDay);
        return new MarketHistoryPoint(worldDay, history.get(worldDay).longValue(),
                previous == null ? null : previous.getValue());
    }

    /**
     * Advances the independent emerald price from the same lazily accessed
     * world-day clock as ordinary goods.  A newly introduced NBT segment is
     * initialized for the current day and deliberately does not roll once in
     * that same access, which keeps upgraded worlds at exactly 1000 initially.
     */
    private boolean ensureEmeraldMarketDay(long worldDay) {
        if (!emeraldMarketInitialized) {
            emeraldMarketInitialized = true;
            emeraldCurrentPrice = EmeraldMarketPriceGenerator.INITIAL_PRICE;
            emeraldPreviousPrice = EmeraldMarketPriceGenerator.INITIAL_PRICE;
            emeraldLastUpdateDay = worldDay;
            appendEmeraldHistory(emeraldLastUpdateDay, emeraldCurrentPrice);
            return true;
        }
        if (worldDay <= emeraldLastUpdateDay) {
            // A server administrator can turn time backwards. The spot market
            // never rerolls, but v10 and earlier saves need one truthful point
            // for their already-persisted current price.
            return appendEmeraldHistory(emeraldLastUpdateDay, emeraldCurrentPrice);
        }
        boolean changed = false;
        while (emeraldLastUpdateDay < worldDay) {
            if (emeraldLastUpdateDay == Long.MAX_VALUE) {
                break;
            }
            long nextDay = emeraldLastUpdateDay + 1L;
            emeraldPreviousPrice = emeraldCurrentPrice;
            emeraldCurrentPrice = EmeraldMarketPriceGenerator.calculateNextPrice(
                    marketSeed, nextDay, emeraldCurrentPrice
            );
            emeraldLastUpdateDay = nextDay;
            appendEmeraldHistory(emeraldLastUpdateDay, emeraldCurrentPrice);
            changed = true;
        }
        return changed;
    }

    private boolean appendEmeraldHistory(long day, long price) {
        if (day < 0L || price < EmeraldMarketPriceGenerator.MINIMUM_PRICE
                || price > EmeraldMarketPriceGenerator.MAXIMUM_PRICE) {
            return false;
        }
        Long previous = emeraldPriceHistory.put(Long.valueOf(day), Long.valueOf(price));
        trimCropHistory(emeraldPriceHistory);
        return previous == null || previous.longValue() != price;
    }

    private void clearMarketData() {
        marketInitialized = false;
        marketSeed = 0L;
        firstMarketDay = -1L;
        currentMarketDay = -1L;
        previousMarketDay = -1L;
        currentMarketPrices.clear();
        previousMarketPrices.clear();
        cropPriceHistory.clear();
        emeraldMarketInitialized = false;
        emeraldCurrentPrice = EmeraldMarketPriceGenerator.INITIAL_PRICE;
        emeraldPreviousPrice = EmeraldMarketPriceGenerator.INITIAL_PRICE;
        emeraldLastUpdateDay = -1L;
        emeraldPriceHistory.clear();
    }

    private NBTTagCompound writeMarket() {
        NBTTagCompound market = new NBTTagCompound();
        market.setBoolean(KEY_MARKET_INITIALIZED, marketInitialized);
        if (!marketInitialized) {
            return market;
        }
        market.setLong(KEY_MARKET_SEED, marketSeed);
        market.setLong(KEY_FIRST_MARKET_DAY, firstMarketDay);
        market.setTag(KEY_CURRENT_SNAPSHOT, writeSnapshot(currentMarketDay, currentMarketPrices));
        if (previousMarketDay >= 0L) {
            market.setTag(KEY_PREVIOUS_SNAPSHOT, writeSnapshot(previousMarketDay, previousMarketPrices));
        }
        market.setTag(KEY_CROP_HISTORIES, writeCropHistories());
        market.setBoolean(KEY_EMERALD_MARKET_INITIALIZED, emeraldMarketInitialized);
        if (emeraldMarketInitialized) {
            market.setLong(KEY_EMERALD_CURRENT_PRICE, emeraldCurrentPrice);
            market.setLong(KEY_EMERALD_PREVIOUS_PRICE, emeraldPreviousPrice);
            market.setLong(KEY_EMERALD_LAST_UPDATE_DAY, emeraldLastUpdateDay);
            market.setTag(KEY_EMERALD_HISTORY, writeEmeraldHistory());
        }
        return market;
    }

    private NBTTagCompound writeSnapshot(long day, Map<String, Long> prices) {
        NBTTagCompound snapshot = new NBTTagCompound();
        snapshot.setLong(KEY_DAY, day);
        NBTTagList values = new NBTTagList();
        for (Map.Entry<String, Long> price : prices.entrySet()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString(KEY_PRICE_KEY, price.getKey());
            entry.setLong(KEY_PRICE_VALUE, price.getValue().longValue());
            values.appendTag(entry);
        }
        snapshot.setTag(KEY_SNAPSHOT_PRICES, values);
        return snapshot;
    }

    private NBTTagList writeCropHistories() {
        NBTTagList histories = new NBTTagList();
        for (Map.Entry<String, NavigableMap<Long, Long>> history : cropPriceHistory.entrySet()) {
            NBTTagCompound serializedHistory = new NBTTagCompound();
            serializedHistory.setString(KEY_HISTORY_KEY, history.getKey());
            NBTTagList points = new NBTTagList();
            for (Map.Entry<Long, Long> point : history.getValue().entrySet()) {
                NBTTagCompound serializedPoint = new NBTTagCompound();
                serializedPoint.setLong(KEY_DAY, point.getKey().longValue());
                serializedPoint.setLong(KEY_PRICE_VALUE, point.getValue().longValue());
                points.appendTag(serializedPoint);
            }
            serializedHistory.setTag(KEY_HISTORY_POINTS, points);
            histories.appendTag(serializedHistory);
        }
        return histories;
    }

    private NBTTagList writeEmeraldHistory() {
        NBTTagList points = new NBTTagList();
        for (Map.Entry<Long, Long> point : emeraldPriceHistory.entrySet()) {
            NBTTagCompound serializedPoint = new NBTTagCompound();
            serializedPoint.setLong(KEY_DAY, point.getKey().longValue());
            serializedPoint.setLong(KEY_PRICE_VALUE, point.getValue().longValue());
            points.appendTag(serializedPoint);
        }
        return points;
    }

    private void readMarket(NBTTagCompound market) {
        marketInitialized = market.getBoolean(KEY_MARKET_INITIALIZED);
        if (!marketInitialized) {
            return;
        }
        marketSeed = market.getLong(KEY_MARKET_SEED);
        firstMarketDay = market.getLong(KEY_FIRST_MARKET_DAY);
        readSnapshot(market.getCompoundTag(KEY_CURRENT_SNAPSHOT), currentMarketPrices, true);
        if (market.hasKey(KEY_PREVIOUS_SNAPSHOT, 10)) {
            readSnapshot(market.getCompoundTag(KEY_PREVIOUS_SNAPSHOT), previousMarketPrices, false);
        }
        readCropHistories(market.getTagList(KEY_CROP_HISTORIES, 10));
        if (market.getBoolean(KEY_EMERALD_MARKET_INITIALIZED)
                && market.hasKey(KEY_EMERALD_CURRENT_PRICE, 4)
                && market.hasKey(KEY_EMERALD_PREVIOUS_PRICE, 4)
                && market.hasKey(KEY_EMERALD_LAST_UPDATE_DAY, 4)) {
            long current = market.getLong(KEY_EMERALD_CURRENT_PRICE);
            long previous = market.getLong(KEY_EMERALD_PREVIOUS_PRICE);
            long day = market.getLong(KEY_EMERALD_LAST_UPDATE_DAY);
            if (current >= EmeraldMarketPriceGenerator.MINIMUM_PRICE
                    && current <= EmeraldMarketPriceGenerator.MAXIMUM_PRICE
                    && previous >= EmeraldMarketPriceGenerator.MINIMUM_PRICE
                    && previous <= EmeraldMarketPriceGenerator.MAXIMUM_PRICE && day >= 0L) {
                emeraldMarketInitialized = true;
                emeraldCurrentPrice = current;
                emeraldPreviousPrice = previous;
                emeraldLastUpdateDay = day;
                readEmeraldHistory(market.getTagList(KEY_EMERALD_HISTORY, 10));
            }
        }
        // Preserve recorded prices exactly. In particular, a catalog base-price
        // adjustment must not rewrite an existing save's current day; the next
        // chained daily step gradually returns that value toward the new base.
    }

    private void readSnapshot(NBTTagCompound snapshot, Map<String, Long> target, boolean current) {
        long day = snapshot.getLong(KEY_DAY);
        if (current) {
            currentMarketDay = day;
        } else {
            previousMarketDay = day;
        }
        NBTTagList values = snapshot.getTagList(KEY_SNAPSHOT_PRICES, 10);
        for (int index = 0; index < values.tagCount(); index++) {
            NBTTagCompound value = values.getCompoundTagAt(index);
            String key = value.getString(KEY_PRICE_KEY);
            long price = value.getLong(KEY_PRICE_VALUE);
            if (!key.isEmpty() && price > 0L && MarketCatalog.get(key) != null) {
                target.put(key, price);
            }
        }
    }

    private void readCropHistories(NBTTagList serializedHistories) {
        for (int index = 0; index < serializedHistories.tagCount(); index++) {
            NBTTagCompound serializedHistory = serializedHistories.getCompoundTagAt(index);
            String key = serializedHistory.getString(KEY_HISTORY_KEY);
            if (key.isEmpty()) {
                continue;
            }
            if (MarketCatalog.get(key) == null || !MarketCatalog.get(key).isHistoryTracked()) {
                continue;
            }
            NavigableMap<Long, Long> history = new TreeMap<Long, Long>();
            NBTTagList points = serializedHistory.getTagList(KEY_HISTORY_POINTS, 10);
            for (int pointIndex = 0; pointIndex < points.tagCount(); pointIndex++) {
                NBTTagCompound point = points.getCompoundTagAt(pointIndex);
                long price = point.getLong(KEY_PRICE_VALUE);
                if (price > 0L) {
                    history.put(point.getLong(KEY_DAY), price);
                    while (history.size() > MARKET_HISTORY_RETENTION_DAYS) {
                        history.pollFirstEntry();
                    }
                }
            }
            cropPriceHistory.put(key, history);
        }
    }

    private void readEmeraldHistory(NBTTagList serializedPoints) {
        for (int index = 0; index < serializedPoints.tagCount(); index++) {
            NBTTagCompound point = serializedPoints.getCompoundTagAt(index);
            long day = point.getLong(KEY_DAY);
            long price = point.getLong(KEY_PRICE_VALUE);
            if (day >= 0L && price >= EmeraldMarketPriceGenerator.MINIMUM_PRICE
                    && price <= EmeraldMarketPriceGenerator.MAXIMUM_PRICE) {
                emeraldPriceHistory.put(Long.valueOf(day), Long.valueOf(price));
                trimCropHistory(emeraldPriceHistory);
            }
        }
    }

}
