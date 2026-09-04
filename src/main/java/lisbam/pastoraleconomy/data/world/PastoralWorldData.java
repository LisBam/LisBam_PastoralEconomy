package lisbam.pastoraleconomy.data.world;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.market.MarketCatalog;
import lisbam.pastoraleconomy.market.MarketCommodity;
import lisbam.pastoraleconomy.market.MarketHistoryPoint;
import lisbam.pastoraleconomy.market.MarketPriceSnapshot;
import lisbam.pastoraleconomy.market.MarketPriceGenerator;
import lisbam.pastoraleconomy.merchant.MerchantWorldState;
import lisbam.pastoraleconomy.transport.TransportWorldState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Iterator;
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
    public static final int DATA_VERSION = 7;
    public static final long MARKET_DAY_TICKS = 24000L;
    /** The market book has one 30-day window, so older points must never grow the save. */
    public static final int MARKET_HISTORY_RETENTION_DAYS = 30;
    public static final long FARM_HARASSMENT_WINDOW_TICKS = 6000L;
    public static final int FARM_HARASSMENT_MAXIMUM = 6;
    public static final int FARM_HARASSMENT_RADIUS = 32;

    private static final String KEY_DATA_VERSION = "dataVersion";
    private static final String KEY_INITIALIZED = "firstInitializationCompleted";
    private static final String KEY_MARKET = "market";
    private static final String KEY_MERCHANT = "merchant";
    private static final String KEY_TRANSPORT = "transport";
    private static final String KEY_FARM_HARASSMENT = "farmHarassment";
    private static final String KEY_RECORD_DIMENSION = "dimension";
    private static final String KEY_RECORD_X = "x";
    private static final String KEY_RECORD_Y = "y";
    private static final String KEY_RECORD_Z = "z";
    private static final String KEY_RECORD_TICK = "tick";
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

    private int dataVersion = DATA_VERSION;
    private boolean firstInitializationCompleted;
    private final List<FarmHarassmentRecord> farmHarassmentRecords = new ArrayList<FarmHarassmentRecord>();
    private boolean marketInitialized;
    private long marketSeed;
    private long firstMarketDay = -1L;
    private long currentMarketDay = -1L;
    private long previousMarketDay = -1L;
    private final Map<String, Long> currentMarketPrices = new LinkedHashMap<String, Long>();
    private final Map<String, Long> previousMarketPrices = new LinkedHashMap<String, Long>();
    private final Map<String, NavigableMap<Long, Long>> cropPriceHistory =
            new LinkedHashMap<String, NavigableMap<Long, Long>>();
    /** Runtime-only marker. Loaded v3-v6 data is normalized on its first market access. */
    private long cropHistoryWindowDay = Long.MIN_VALUE;
    /** Batch 08--10 world-owned village, station, merchant, offer, and stock state. */
    private final MerchantWorldState merchantWorldState = new MerchantWorldState();
    /** Batch 14 world-owned physical transport-node registry. */
    private final TransportWorldState transportWorldState = new TransportWorldState();

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
        if (data.pruneFarmHarassment(overworld.getTotalWorldTime())) {
            data.markDirty();
        }
        return data;
    }

    /** Shared server tick source used by cross-dimension harassment quota data. */
    public static long getAuthoritativeTick(World world) {
        return getOverworld(world).getTotalWorldTime();
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
     * Server-side market transition. Prices are deterministic from the stable
     * market seed, so a time jump never needs to iterate every skipped day.
     * Only the visible 30-day crop window is materialized and persisted.
     */
    public synchronized void ensureMarketDay(long worldDay, long authoritativeWorldSeed) {
        if (!marketInitialized) {
            marketInitialized = true;
            marketSeed = MarketPriceGenerator.createMarketSeed(authoritativeWorldSeed);
            firstMarketDay = worldDay;
            setCurrentSnapshot(worldDay, Collections.<String, Long>emptyMap());
            previousMarketDay = -1L;
            previousMarketPrices.clear();
            rebuildCropHistoryWindow(worldDay);
            markDirty();
            return;
        }

        if (worldDay == currentMarketDay) {
            boolean changed = fillMissingCurrentPrices();
            if (cropHistoryWindowDay != worldDay) {
                rebuildCropHistoryWindow(worldDay);
                changed = true;
            }
            if (changed) {
                markDirty();
            }
            return;
        }

        setCurrentSnapshot(worldDay, Collections.<String, Long>emptyMap());
        previousMarketPrices.clear();
        if (worldDay > firstMarketDay) {
            previousMarketDay = worldDay - 1L;
            previousMarketPrices.putAll(createSnapshot(previousMarketDay, Collections.<String, Long>emptyMap()));
        } else {
            // A rollback before market activation is viewable but must not
            // invent pre-install history.
            previousMarketDay = -1L;
        }
        rebuildCropHistoryWindow(worldDay);
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

    /** One immutable price view avoids repeated WorldSavedData lookups per merchant GUI snapshot. */
    public synchronized MarketPriceSnapshot createMarketPriceSnapshot() {
        return new MarketPriceSnapshot(currentMarketDay,
                new LinkedHashMap<String, Long>(currentMarketPrices),
                new LinkedHashMap<String, Long>(previousMarketPrices));
    }

    public synchronized List<MarketHistoryPoint> getCropHistory(String key, long visibleThroughDay, int limit,
                                                                 Long beforeExclusiveDay) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        NavigableMap<Long, Long> history = cropPriceHistory.get(key);
        if (history == null || history.isEmpty()) {
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

    public synchronized MarketHistoryPoint getCropHistoryPoint(String key, long worldDay, long visibleThroughDay) {
        if (worldDay > visibleThroughDay) {
            return null;
        }
        NavigableMap<Long, Long> history = cropPriceHistory.get(key);
        if (history == null || !history.containsKey(worldDay)) {
            return null;
        }
        Map.Entry<Long, Long> previous = history.lowerEntry(worldDay);
        return new MarketHistoryPoint(worldDay, history.get(worldDay).longValue(),
                previous == null ? null : previous.getValue());
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

    /** True when this dimension/position still has room under the shared six-event quota. */
    public boolean canRecordFarmHarassment(int dimension, BlockPos position, long currentTick) {
        if (pruneFarmHarassment(currentTick)) {
            markDirty();
        }

        int nearbyCount = 0;
        long radiusSq = (long) FARM_HARASSMENT_RADIUS * (long) FARM_HARASSMENT_RADIUS;
        for (FarmHarassmentRecord record : farmHarassmentRecords) {
            if (record.dimension != dimension) {
                continue;
            }
            long deltaX = (long) record.x - (long) position.getX();
            long deltaZ = (long) record.z - (long) position.getZ();
            if (deltaX * deltaX + deltaZ * deltaZ <= radiusSq) {
                nearbyCount++;
                if (nearbyCount >= FARM_HARASSMENT_MAXIMUM) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Records only a block change that has already succeeded on the logical server. */
    public void recordFarmHarassment(int dimension, BlockPos position, long currentTick) {
        if (pruneFarmHarassment(currentTick)) {
            markDirty();
        }
        farmHarassmentRecords.add(new FarmHarassmentRecord(
                dimension,
                position.getX(),
                position.getY(),
                position.getZ(),
                currentTick
        ));
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        int storedVersion = compound.hasKey(KEY_DATA_VERSION) ? compound.getInteger(KEY_DATA_VERSION) : 1;
        firstInitializationCompleted = compound.getBoolean(KEY_INITIALIZED);
        farmHarassmentRecords.clear();
        clearMarketData();
        merchantWorldState.readFromNBT(new NBTTagCompound());
        transportWorldState.readFromNBT(new NBTTagCompound());

        // v1 contains no farmHarassment section. Its keepInventory marker and
        // all reserved sections remain intact while v2 starts with no records.
        if (storedVersion >= 2) {
            NBTTagList serializedRecords = compound.getTagList(KEY_FARM_HARASSMENT, 10);
            for (int index = 0; index < serializedRecords.tagCount(); index++) {
                NBTTagCompound serializedRecord = serializedRecords.getCompoundTagAt(index);
                farmHarassmentRecords.add(new FarmHarassmentRecord(
                        serializedRecord.getInteger(KEY_RECORD_DIMENSION),
                        serializedRecord.getInteger(KEY_RECORD_X),
                        serializedRecord.getInteger(KEY_RECORD_Y),
                        serializedRecord.getInteger(KEY_RECORD_Z),
                        serializedRecord.getLong(KEY_RECORD_TICK)
                ));
            }
        }
        if (storedVersion >= 3 && compound.hasKey(KEY_MARKET, 10)) {
            readMarket(compound.getCompoundTag(KEY_MARKET));
        }
        if (storedVersion >= 4 && compound.hasKey(KEY_MERCHANT, 10)) {
            merchantWorldState.readFromNBT(compound.getCompoundTag(KEY_MERCHANT));
        }
        if (storedVersion >= 6 && compound.hasKey(KEY_TRANSPORT, 10)) {
            transportWorldState.readFromNBT(compound.getCompoundTag(KEY_TRANSPORT));
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
        compound.setTag(KEY_FARM_HARASSMENT, writeFarmHarassmentRecords());
        return compound;
    }

    private NBTTagList writeFarmHarassmentRecords() {
        NBTTagList serializedRecords = new NBTTagList();
        for (FarmHarassmentRecord record : farmHarassmentRecords) {
            NBTTagCompound serializedRecord = new NBTTagCompound();
            serializedRecord.setInteger(KEY_RECORD_DIMENSION, record.dimension);
            serializedRecord.setInteger(KEY_RECORD_X, record.x);
            serializedRecord.setInteger(KEY_RECORD_Y, record.y);
            serializedRecord.setInteger(KEY_RECORD_Z, record.z);
            serializedRecord.setLong(KEY_RECORD_TICK, record.tick);
            serializedRecords.appendTag(serializedRecord);
        }
        return serializedRecords;
    }

    private void setCurrentSnapshot(long day, Map<String, Long> existingPrices) {
        currentMarketDay = day;
        currentMarketPrices.clear();
        currentMarketPrices.putAll(createSnapshot(day, existingPrices));
    }

    private boolean fillMissingCurrentPrices() {
        boolean changed = false;
        for (MarketCommodity commodity : MarketCatalog.getAll()) {
            if (!currentMarketPrices.containsKey(commodity.getKey())) {
                currentMarketPrices.put(commodity.getKey(), MarketPriceGenerator.calculatePrice(marketSeed, currentMarketDay, commodity));
                changed = true;
            }
        }
        return changed;
    }

    private Map<String, Long> createSnapshot(long day, Map<String, Long> existingPrices) {
        Map<String, Long> snapshot = new LinkedHashMap<String, Long>(existingPrices);
        for (MarketCommodity commodity : MarketCatalog.getAll()) {
            if (!snapshot.containsKey(commodity.getKey())) {
                snapshot.put(commodity.getKey(), MarketPriceGenerator.calculatePrice(marketSeed, day, commodity));
            }
        }
        return snapshot;
    }

    private void rebuildCropHistoryWindow(long worldDay) {
        cropPriceHistory.clear();
        cropHistoryWindowDay = worldDay;
        if (worldDay < firstMarketDay) {
            return;
        }
        long retentionOffset = MARKET_HISTORY_RETENTION_DAYS - 1L;
        long earliestRetainedDay = worldDay < Long.MIN_VALUE + retentionOffset
                ? Long.MIN_VALUE : worldDay - retentionOffset;
        long firstVisibleDay = Math.max(firstMarketDay, earliestRetainedDay);
        for (MarketCommodity commodity : MarketCatalog.getHistoryTracked()) {
            NavigableMap<Long, Long> history = new TreeMap<Long, Long>();
            for (long historyDay = firstVisibleDay; historyDay <= worldDay; historyDay++) {
                history.put(historyDay, MarketPriceGenerator.calculatePrice(marketSeed, historyDay, commodity));
                if (historyDay == Long.MAX_VALUE) {
                    break;
                }
            }
            cropPriceHistory.put(commodity.getKey(), history);
        }
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
        cropHistoryWindowDay = Long.MIN_VALUE;
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
        // v3-v6 saves may carry unbounded history. Retain no more than the
        // newest thirty entries while the first on-demand market access
        // deterministically rebuilds the exact current-day window.
        cropHistoryWindowDay = Long.MIN_VALUE;
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

    private boolean pruneFarmHarassment(long currentTick) {
        boolean removed = false;
        Iterator<FarmHarassmentRecord> iterator = farmHarassmentRecords.iterator();
        while (iterator.hasNext()) {
            FarmHarassmentRecord record = iterator.next();
            if (record.tick > currentTick || currentTick - record.tick > FARM_HARASSMENT_WINDOW_TICKS) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    private static final class FarmHarassmentRecord {
        private final int dimension;
        private final int x;
        private final int y;
        private final int z;
        private final long tick;

        private FarmHarassmentRecord(int dimension, int x, int y, int z, long tick) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.tick = tick;
        }
    }
}
