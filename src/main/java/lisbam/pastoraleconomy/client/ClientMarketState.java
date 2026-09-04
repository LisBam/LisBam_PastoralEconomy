package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.market.MarketHistorySnapshot;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** Client-only transient cache keyed by commodity and exclusive page cursor. */
public final class ClientMarketState {
    private static final Map<WindowKey, MarketHistorySnapshot> SNAPSHOTS =
            new HashMap<WindowKey, MarketHistorySnapshot>();

    private ClientMarketState() {
    }

    public static void acceptSnapshot(MarketHistorySnapshot snapshot) {
        WindowKey key = new WindowKey(snapshot.getCommodityKey(), snapshot.getBeforeExclusiveDay());
        MarketHistorySnapshot existing = SNAPSHOTS.get(key);
        if (existing == null || snapshot.getRequestId() >= existing.getRequestId()) {
            SNAPSHOTS.put(key, snapshot);
        }
    }

    @Nullable
    public static MarketHistorySnapshot getSnapshot(String commodityKey, long beforeExclusiveDay, int requestId) {
        MarketHistorySnapshot snapshot = SNAPSHOTS.get(new WindowKey(commodityKey, beforeExclusiveDay));
        return snapshot != null && snapshot.getRequestId() == requestId ? snapshot : null;
    }

    public static void clear() {
        SNAPSHOTS.clear();
    }

    private static final class WindowKey {
        private final String commodityKey;
        private final long beforeExclusiveDay;

        private WindowKey(String commodityKey, long beforeExclusiveDay) {
            this.commodityKey = commodityKey;
            this.beforeExclusiveDay = beforeExclusiveDay;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof WindowKey)) {
                return false;
            }
            WindowKey other = (WindowKey) object;
            return beforeExclusiveDay == other.beforeExclusiveDay && commodityKey.equals(other.commodityKey);
        }

        @Override
        public int hashCode() {
            int result = commodityKey.hashCode();
            result = 31 * result + (int) (beforeExclusiveDay ^ (beforeExclusiveDay >>> 32));
            return result;
        }
    }
}
