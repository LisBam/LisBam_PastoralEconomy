package lisbam.pastoraleconomy.market;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable server-created current/previous price view for one market day. */
public final class MarketPriceSnapshot {
    private final long marketDay;
    private final Map<String, Long> currentPrices;
    private final Map<String, Long> previousPrices;

    public MarketPriceSnapshot(long marketDay, Map<String, Long> currentPrices, Map<String, Long> previousPrices) {
        this.marketDay = marketDay;
        this.currentPrices = Collections.unmodifiableMap(new LinkedHashMap<String, Long>(currentPrices));
        this.previousPrices = Collections.unmodifiableMap(new LinkedHashMap<String, Long>(previousPrices));
    }

    public long getMarketDay() {
        return marketDay;
    }

    @Nullable
    public Long getCurrentPrice(String key) {
        return currentPrices.get(key);
    }

    @Nullable
    public Long getPreviousPrice(String key) {
        return previousPrices.get(key);
    }
}
