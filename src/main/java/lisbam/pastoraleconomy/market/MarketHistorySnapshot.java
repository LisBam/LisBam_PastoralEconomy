package lisbam.pastoraleconomy.market;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bounded, immutable server market view intended for display synchronization.
 * It contains no mutable market state and is never saved on the client.
 */
public final class MarketHistorySnapshot {
    private final int requestId;
    private final String commodityKey;
    private final long beforeExclusiveDay;
    private final long currentMarketDay;
    private final long currentPrice;
    @Nullable
    private final Long previousPrice;
    private final List<MarketHistoryPoint> points;
    private final boolean hasOlderHistory;
    private final boolean hasNewerHistory;

    public MarketHistorySnapshot(
            int requestId,
            String commodityKey,
            long beforeExclusiveDay,
            long currentMarketDay,
            long currentPrice,
            @Nullable Long previousPrice,
            List<MarketHistoryPoint> points,
            boolean hasOlderHistory,
            boolean hasNewerHistory
    ) {
        if (commodityKey == null || commodityKey.length() == 0) {
            throw new IllegalArgumentException("Market snapshot commodity key is required.");
        }
        if (beforeExclusiveDay < -1L || (beforeExclusiveDay >= 0L && beforeExclusiveDay > currentMarketDay)
                || currentMarketDay < 0L || currentPrice <= 0L) {
            throw new IllegalArgumentException("Invalid market snapshot range or current price.");
        }
        if (previousPrice != null && previousPrice.longValue() <= 0L) {
            throw new IllegalArgumentException("Invalid previous market price.");
        }
        if (points == null || points.size() > MarketService.DEFAULT_HISTORY_DAYS) {
            throw new IllegalArgumentException("Market snapshot history must contain at most thirty points.");
        }

        long previousDay = -1L;
        for (MarketHistoryPoint point : points) {
            if (point == null || point.getWorldDay() < 0L || point.getWorldDay() > currentMarketDay
                    || point.getWorldDay() <= previousDay || point.getPrice() <= 0L) {
                throw new IllegalArgumentException("Invalid market history point in snapshot.");
            }
            previousDay = point.getWorldDay();
        }

        this.requestId = requestId;
        this.commodityKey = commodityKey;
        this.beforeExclusiveDay = beforeExclusiveDay;
        this.currentMarketDay = currentMarketDay;
        this.currentPrice = currentPrice;
        this.previousPrice = previousPrice;
        this.points = Collections.unmodifiableList(new ArrayList<MarketHistoryPoint>(points));
        this.hasOlderHistory = hasOlderHistory;
        this.hasNewerHistory = hasNewerHistory;
    }

    public int getRequestId() {
        return requestId;
    }

    public String getCommodityKey() {
        return commodityKey;
    }

    /** -1 represents the newest window; all other values are exclusive cursors. */
    public long getBeforeExclusiveDay() {
        return beforeExclusiveDay;
    }

    public long getCurrentMarketDay() {
        return currentMarketDay;
    }

    public long getCurrentPrice() {
        return currentPrice;
    }

    @Nullable
    public Long getPreviousPrice() {
        return previousPrice;
    }

    public List<MarketHistoryPoint> getPoints() {
        return points;
    }

    public boolean hasOlderHistory() {
        return hasOlderHistory;
    }

    public boolean hasNewerHistory() {
        return hasNewerHistory;
    }
}
