package lisbam.pastoraleconomy.market;

import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The sole read-only market entry point for later merchants and market UI.
 * Every stateful lookup resolves the authoritative shared WorldSavedData.
 */
public final class MarketService {
    public static final int DEFAULT_HISTORY_DAYS = PastoralWorldData.MARKET_HISTORY_RETENTION_DAYS;

    private MarketService() {
    }

    public static List<MarketCommodity> getAllCommodities() {
        return MarketCatalog.getAll();
    }

    @Nullable
    public static MarketCommodity getCommodity(String key) {
        return MarketCatalog.get(key);
    }

    @Nullable
    public static MarketCommodity getSellCommodity(ItemStack stack) {
        return MarketCatalog.findSellCommodity(stack);
    }

    public static boolean isSellCommodity(MarketCommodity commodity) {
        return MarketCatalog.isSellCommodity(commodity);
    }

    public static long getCurrentMarketDay(World world) {
        return getReadyData(world).getCurrentMarketDay();
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
     * The market has no background ticker: an open market book or an economy
     * operation is what lazily refreshes this deterministic daily snapshot.
     */
    public static MarketHistorySnapshot getHistorySnapshot(
            World world, String commodityKey, long beforeExclusiveDay, int limit, int requestId
    ) {
        if (limit <= 0 || limit > DEFAULT_HISTORY_DAYS) {
            throw new IllegalArgumentException("Market history window limit must be between 1 and 30.");
        }

        MarketCommodity commodity = requireHistoryCommodity(commodityKey);
        PastoralWorldData data = getReadyData(world);
        long currentDay = data.getCurrentMarketDay();
        if (beforeExclusiveDay < -1L || (beforeExclusiveDay >= 0L && beforeExclusiveDay > currentDay)) {
            throw new IllegalArgumentException("Market history cursor is outside the current world day.");
        }

        return createHistorySnapshot(data, commodity, beforeExclusiveDay, limit, requestId);
    }

    /**
     * Builds every selectable newest window from one ready WorldSavedData view.
     * A book only needs this once per open session, so merchant-sell good switching can stay
     * entirely client-side without creating one packet round trip per button.
     */
    public static List<MarketHistorySnapshot> getNewestHistorySnapshots(World world, int requestId) {
        PastoralWorldData data = getReadyData(world);
        List<MarketHistorySnapshot> snapshots = new ArrayList<MarketHistorySnapshot>(MarketCatalog.getHistoryTracked().size());
        for (MarketCommodity commodity : MarketCatalog.getHistoryTracked()) {
            snapshots.add(createHistorySnapshot(data, commodity, -1L, DEFAULT_HISTORY_DAYS, requestId));
        }
        return Collections.unmodifiableList(snapshots);
    }

    private static MarketHistorySnapshot createHistorySnapshot(
            PastoralWorldData data, MarketCommodity commodity, long beforeExclusiveDay, int limit, int requestId
    ) {
        long currentDay = data.getCurrentMarketDay();
        Long currentPrice = data.getCurrentMarketPrice(commodity.getKey());
        if (currentPrice == null) {
            throw new IllegalStateException("Current frozen market snapshot is incomplete: " + commodity.getKey());
        }
        List<MarketHistoryPoint> points = beforeExclusiveDay == -1L
                ? data.getCropHistory(commodity.getKey(), currentDay, limit, null)
                : data.getCropHistory(commodity.getKey(), currentDay, limit, Long.valueOf(beforeExclusiveDay));
        // A legacy save can have a frozen current price but no retained list
        // for a commodity introduced later. getReadyData repairs that list on
        // the logical server; this invariant prevents its first valid newest
        // snapshot from being rendered as an empty market history.
        if (beforeExclusiveDay == -1L && points.isEmpty()) {
            points = Collections.singletonList(new MarketHistoryPoint(currentDay, currentPrice.longValue(),
                    data.getPreviousMarketPrice(commodity.getKey())));
        }
        boolean hasOlder = !points.isEmpty()
                && !data.getCropHistory(commodity.getKey(), currentDay, 1,
                Long.valueOf(points.get(0).getWorldDay())).isEmpty();
        boolean hasNewer = beforeExclusiveDay != -1L && !points.isEmpty();

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

    /**
     * Captures all current/previous prices once for callers that need to render
     * many offers. This avoids resolving WorldSavedData for every offer row.
     */
    public static MarketPriceSnapshot getPriceSnapshot(World world) {
        return getReadyData(world).createMarketPriceSnapshot();
    }

    /** Current shared emerald/coin spot price, refreshed only when the market is actually read. */
    public static long getEmeraldCurrentPrice(World world) {
        return getReadyData(world).getEmeraldCurrentPrice();
    }

    /** Previous shared emerald/coin spot price for today's delta display. */
    public static long getEmeraldPreviousPrice(World world) {
        return getReadyData(world).getEmeraldPreviousPrice();
    }

    private static PastoralWorldData getReadyData(World world) {
        PastoralWorldData data = PastoralWorldData.get(world);
        data.ensureMarketDay(
                PastoralWorldData.getAuthoritativeMarketDay(world),
                PastoralWorldData.getAuthoritativeWorldSeed(world)
        );
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
            throw new IllegalArgumentException("Commodity has no merchant-sell price history: " + commodityKey);
        }
        return commodity;
    }
}
