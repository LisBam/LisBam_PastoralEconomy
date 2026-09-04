package lisbam.pastoraleconomy.market;

import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The sole read-only market entry point for later merchants and market UI.
 * Every stateful lookup resolves the authoritative shared WorldSavedData.
 */
public final class MarketService {
    public static final int DEFAULT_HISTORY_DAYS = 30;

    private MarketService() {
    }

    /** Lightweight server tick hook; only the overworld calls it. */
    public static void tick(WorldServer overworld) {
        if (overworld.provider.getDimension() != 0) {
            return;
        }
        getReadyData(overworld);
    }

    public static List<MarketCommodity> getAllCommodities() {
        return MarketCatalog.getAll();
    }

    @Nullable
    public static MarketCommodity getCommodity(String key) {
        return MarketCatalog.get(key);
    }

    public static long getCurrentMarketDay(World world) {
        return getReadyData(world).getCurrentMarketDay();
    }

    /** Display-only lookup that never advances the market as a side effect. */
    public static long getCurrentMarketDayForDisplay(World world) {
        return getReadOnlyMarketData(world).getCurrentMarketDay();
    }

    public static long getCurrentPrice(World world, String commodityKey) {
        MarketCommodity commodity = requireCommodity(commodityKey);
        Long price = getReadyData(world).getCurrentMarketPrice(commodity.getKey());
        if (price == null) {
            throw new IllegalStateException("Current frozen market snapshot is incomplete: " + commodity.getKey());
        }
        return price.longValue();
    }

    @Nullable
    public static Long getPreviousPrice(World world, String commodityKey) {
        return getReadyData(world).getPreviousMarketPrice(requireCommodity(commodityKey).getKey());
    }

    public static boolean hasPreviousPrice(World world, String commodityKey) {
        requireCommodity(commodityKey);
        return getReadyData(world).hasPreviousMarketSnapshot();
    }

    public static MarketTrend getCurrentTrend(World world, String commodityKey) {
        long current = getCurrentPrice(world, commodityKey);
        Long previous = getPreviousPrice(world, commodityKey);
        return previous == null ? MarketTrend.FLAT : MarketTrend.compare(current, previous.longValue());
    }

    public static List<MarketHistoryPoint> getRecentCropHistory(World world, String commodityKey) {
        return getRecentCropHistory(world, commodityKey, DEFAULT_HISTORY_DAYS);
    }

    public static List<MarketHistoryPoint> getRecentCropHistory(World world, String commodityKey, int limit) {
        MarketCommodity commodity = requireHistoryCommodity(commodityKey);
        PastoralWorldData data = getReadyData(world);
        return data.getCropHistory(commodity.getKey(), data.getCurrentMarketDay(), limit, null);
    }

    /** Returns one earlier page, ordered chronologically, without exposing future days. */
    public static List<MarketHistoryPoint> getCropHistoryBefore(
            World world, String commodityKey, long beforeExclusiveDay, int limit
    ) {
        MarketCommodity commodity = requireHistoryCommodity(commodityKey);
        PastoralWorldData data = getReadyData(world);
        return data.getCropHistory(commodity.getKey(), data.getCurrentMarketDay(), limit, Long.valueOf(beforeExclusiveDay));
    }

    /**
     * Builds one bounded display window from the authoritative WorldSavedData.
     * This is a read-only view: the normal overworld tick remains the only
     * active market-day advancement entry point.
     */
    public static MarketHistorySnapshot getHistorySnapshot(
            World world, String commodityKey, long beforeExclusiveDay, int limit, int requestId
    ) {
        if (limit <= 0 || limit > DEFAULT_HISTORY_DAYS) {
            throw new IllegalArgumentException("Market history window limit must be between 1 and 30.");
        }

        MarketCommodity commodity = requireHistoryCommodity(commodityKey);
        PastoralWorldData data = getReadOnlyMarketData(world);
        long currentDay = data.getCurrentMarketDay();
        if (beforeExclusiveDay < -1L || (beforeExclusiveDay >= 0L && beforeExclusiveDay > currentDay)) {
            throw new IllegalArgumentException("Market history cursor is outside the current world day.");
        }

        List<MarketHistoryPoint> points = beforeExclusiveDay == -1L
                ? data.getCropHistory(commodity.getKey(), currentDay, limit, null)
                : data.getCropHistory(commodity.getKey(), currentDay, limit, Long.valueOf(beforeExclusiveDay));
        boolean hasOlder = !points.isEmpty()
                && !data.getCropHistory(commodity.getKey(), currentDay, 1,
                Long.valueOf(points.get(0).getWorldDay())).isEmpty();
        boolean hasNewer = beforeExclusiveDay != -1L && !points.isEmpty();
        Long currentPrice = data.getCurrentMarketPrice(commodity.getKey());
        if (currentPrice == null) {
            throw new IllegalStateException("Current frozen market snapshot is incomplete: " + commodity.getKey());
        }

        return new MarketHistorySnapshot(
                requestId,
                commodity.getKey(),
                beforeExclusiveDay,
                currentDay,
                currentPrice.longValue(),
                data.getPreviousMarketPrice(commodity.getKey()),
                points,
                hasOlder,
                hasNewer
        );
    }

    @Nullable
    public static MarketHistoryPoint getCropHistoryPoint(World world, String commodityKey, long worldDay) {
        MarketCommodity commodity = requireHistoryCommodity(commodityKey);
        PastoralWorldData data = getReadyData(world);
        return data.getCropHistoryPoint(commodity.getKey(), worldDay, data.getCurrentMarketDay());
    }

    private static PastoralWorldData getReadyData(World world) {
        PastoralWorldData data = PastoralWorldData.get(world);
        data.ensureMarketDay(
                PastoralWorldData.getAuthoritativeMarketDay(world),
                PastoralWorldData.getAuthoritativeWorldSeed(world)
        );
        return data;
    }

    /** Used by display packets so opening a GUI never advances or initializes market history. */
    private static PastoralWorldData getReadOnlyMarketData(World world) {
        PastoralWorldData data = PastoralWorldData.get(world);
        if (!data.isMarketInitialized()) {
            throw new IllegalStateException("Market data has not been initialized by the overworld tick.");
        }
        return data;
    }

    private static MarketCommodity requireCommodity(String commodityKey) {
        MarketCommodity commodity = MarketCatalog.get(commodityKey);
        if (commodity == null) {
            throw new IllegalArgumentException("Unknown market commodity: " + commodityKey);
        }
        return commodity;
    }

    private static MarketCommodity requireHistoryCommodity(String commodityKey) {
        MarketCommodity commodity = requireCommodity(commodityKey);
        if (!commodity.isHistoryTracked()) {
            throw new IllegalArgumentException("Commodity has no crop price history: " + commodityKey);
        }
        return commodity;
    }
}
