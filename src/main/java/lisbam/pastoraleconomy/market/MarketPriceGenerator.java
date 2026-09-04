package lisbam.pastoraleconomy.market;

/** Pure deterministic market-price formula. It owns the only final rounding step. */
public final class MarketPriceGenerator {
    private static final long MARKET_SEED_SALT = 0x4c697342616d4d6fL;
    private static final long FIRST_RANDOM_SALT = 0x6d61726b65742d31L;
    private static final long SECOND_RANDOM_SALT = 0x6d61726b65742d32L;
    private static final double UNIT_53 = 0x1.0p-53;

    private MarketPriceGenerator() {
    }

    public static long createMarketSeed(long worldSeed) {
        return mix64(worldSeed ^ MARKET_SEED_SALT);
    }

    public static long calculatePrice(long marketSeed, long worldDay, MarketCommodity commodity) {
        long commodityHash = stableKeyHash(commodity.getKey());
        double first = toUnit(mix64(marketSeed ^ mix64(worldDay) ^ commodityHash ^ FIRST_RANDOM_SALT));
        double second = toUnit(mix64(marketSeed ^ mix64(worldDay) ^ commodityHash ^ SECOND_RANDOM_SALT));
        return calculatePrice(commodity.getBasePrice(), commodity.getCategory(), first, second);
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
