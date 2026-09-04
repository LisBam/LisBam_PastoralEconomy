package lisbam.pastoraleconomy.market;

/** Pure deterministic market-price formula. It owns the only final rounding step. */
public final class MarketPriceGenerator {
    private static final long MARKET_SEED_SALT = 0x4c697342616d4d6fL;
    private static final long FIRST_RANDOM_SALT = 0x6d61726b65742d31L;
    private static final long SECOND_RANDOM_SALT = 0x6d61726b65742d32L;
    private static final long DIRECTION_RANDOM_SALT = 0x6d61726b65742d33L;
    private static final long MAGNITUDE_RANDOM_SALT = 0x6d61726b65742d34L;
    private static final double UNIT_53 = 0x1.0p-53;
    /** A non-base price is more likely to move back toward its initial price. */
    static final double RESTORE_DIRECTION_PROBABILITY = 0.65D;

    private MarketPriceGenerator() {
    }

    public static long createMarketSeed(long worldSeed) {
        return mix64(worldSeed ^ MARKET_SEED_SALT);
    }

    /**
     * The pre-maintenance independent daily formula. It remains public for the
     * opt-in "more stable" configuration and for v7 compatibility checks.
     */
    public static long calculatePrice(long marketSeed, long worldDay, MarketCommodity commodity) {
        long commodityHash = stableKeyHash(commodity.getKey());
        double first = toUnit(mix64(marketSeed ^ mix64(worldDay) ^ commodityHash ^ FIRST_RANDOM_SALT));
        double second = toUnit(mix64(marketSeed ^ mix64(worldDay) ^ commodityHash ^ SECOND_RANDOM_SALT));
        return calculatePrice(commodity.getBasePrice(), commodity.getCategory(), first, second);
    }

    /** The initial chained-market price is exactly the frozen catalog price. */
    public static long calculateInitialPrice(long marketSeed, long worldDay, MarketCommodity commodity,
                                             boolean moreStableVolatility) {
        return moreStableVolatility ? calculatePrice(marketSeed, worldDay, commodity) : commodity.getBasePrice();
    }

    /**
     * Advances exactly one market day. The default branch intentionally uses
     * yesterday's saved price; no fixed band around the base price is applied.
     */
    public static long calculateNextPrice(long marketSeed, long worldDay, MarketCommodity commodity,
                                          long previousPrice, boolean moreStableVolatility) {
        if (moreStableVolatility) {
            return calculatePrice(marketSeed, worldDay, commodity);
        }
        long commodityHash = stableKeyHash(commodity.getKey());
        double direction = toUnit(mix64(marketSeed ^ mix64(worldDay) ^ commodityHash ^ DIRECTION_RANDOM_SALT));
        double magnitude = toUnit(mix64(marketSeed ^ mix64(worldDay) ^ commodityHash ^ MAGNITUDE_RANDOM_SALT));
        return calculateChainedPrice(commodity.getBasePrice(), commodity.getCategory(), previousPrice,
                direction, magnitude);
    }

    static long calculatePrice(long basePrice, CommodityCategory category, double first, double second) {
        if (!isBasePriceSupported(basePrice, category) || first < 0.0D || first >= 1.0D
                || second < 0.0D || second >= 1.0D) {
            throw new IllegalArgumentException("Invalid frozen market price input.");
        }
        double multiplier = 1.0D + (first + second - 1.0D) * category.getVolatility();
        double scaled = basePrice * multiplier;
        if (scaled > Long.MAX_VALUE - 0.5D) {
            throw new IllegalStateException("Frozen market price overflows long.");
        }
        return Math.max(1L, Math.round(scaled));
    }

    /** Pure one-day chained step retained package-visible for deterministic regression checks. */
    static long calculateChainedPrice(long basePrice, CommodityCategory category, long previousPrice,
                                      double direction, double magnitude) {
        if (!isBasePriceSupported(basePrice, category) || previousPrice <= 0L
                || direction < 0.0D || direction >= 1.0D || magnitude < 0.0D || magnitude >= 1.0D) {
            throw new IllegalArgumentException("Invalid chained market price input.");
        }

        boolean increases;
        if (previousPrice < basePrice) {
            increases = direction < RESTORE_DIRECTION_PROBABILITY;
        } else if (previousPrice > basePrice) {
            increases = direction >= RESTORE_DIRECTION_PROBABILITY;
        } else {
            increases = direction < 0.5D;
        }

        // Individual steps remain legible for each commodity category, but
        // repeated steps are not clamped to any permanent percentage band.
        double step = category.getVolatility() * (0.25D + magnitude * 0.75D);
        double multiplier = increases ? 1.0D + step : 1.0D - step;
        double scaled = previousPrice * multiplier;
        if (scaled > Long.MAX_VALUE - 0.5D) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, Math.round(scaled));
    }

    static boolean isBasePriceSupported(long basePrice, CommodityCategory category) {
        if (basePrice <= 0L || category == null) {
            return false;
        }
        return basePrice <= (long) ((Long.MAX_VALUE - 1.0D) / (1.0D + category.getVolatility()));
    }

    private static long stableKeyHash(String key) {
        long hash = 0xcbf29ce484222325L;
        for (int index = 0; index < key.length(); index++) {
            hash ^= key.charAt(index);
            hash *= 0x100000001b3L;
        }
        return mix64(hash);
    }

    private static double toUnit(long value) {
        return (value >>> 11) * UNIT_53;
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }
}
