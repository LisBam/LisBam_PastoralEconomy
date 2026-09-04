package lisbam.pastoraleconomy.market;

import javax.annotation.Nullable;

/** Read-only history DTO. The first actual point deliberately has no delta. */
public final class MarketHistoryPoint {
    private final long worldDay;
    private final long price;
    @Nullable
    private final Long previousPrice;

    public MarketHistoryPoint(long worldDay, long price, @Nullable Long previousPrice) {
        this.worldDay = worldDay;
        this.price = price;
        this.previousPrice = previousPrice;
    }

    public long getWorldDay() {
        return worldDay;
    }

    public long getPrice() {
        return price;
    }

    public boolean hasPreviousPoint() {
        return previousPrice != null;
    }

    @Nullable
    public Long getPreviousPrice() {
        return previousPrice;
    }

    public long getDelta() {
        return previousPrice == null ? 0L : price - previousPrice.longValue();
    }

    public MarketTrend getTrend() {
        return previousPrice == null ? MarketTrend.FLAT : MarketTrend.compare(price, previousPrice.longValue());
    }
}
