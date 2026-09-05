package lisbam.pastoraleconomy.client;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** Transient, server-fed prices for sellable stacks hovered in the player inventory. */
public final class ClientMarketTooltipState {
    private static final Map<String, Price> PRICES = new HashMap<String, Price>();
    private static final Map<String, Long> LAST_REQUEST_TICKS = new HashMap<String, Long>();
    private static final long REQUEST_RETRY_TICKS = 20L;

    private ClientMarketTooltipState() {
    }

    public static void acceptPrice(String commodityKey, long marketDay, long price) {
        if (commodityKey == null || marketDay < 0L || price <= 0L) {
            return;
        }
        PRICES.put(commodityKey, new Price(marketDay, price));
    }

    @Nullable
    public static Long getPriceForDay(String commodityKey, long marketDay) {
        Price price = PRICES.get(commodityKey);
        return price != null && price.marketDay == marketDay ? Long.valueOf(price.value) : null;
    }

    /** Limits a missing tooltip price to one small request per second per commodity. */
    public static boolean shouldRequest(String commodityKey, long clientTick) {
        Long lastRequest = LAST_REQUEST_TICKS.get(commodityKey);
        if (lastRequest != null && clientTick >= lastRequest.longValue()
                && clientTick - lastRequest.longValue() < REQUEST_RETRY_TICKS) {
            return false;
        }
        LAST_REQUEST_TICKS.put(commodityKey, Long.valueOf(clientTick));
        return true;
    }

    public static void clear() {
        PRICES.clear();
        LAST_REQUEST_TICKS.clear();
    }

    private static final class Price {
        private final long marketDay;
        private final long value;

        private Price(long marketDay, long value) {
            this.marketDay = marketDay;
            this.value = value;
        }
    }
}
