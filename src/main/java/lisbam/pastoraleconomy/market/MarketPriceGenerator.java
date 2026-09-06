package lisbam.pastoraleconomy.market;

/** Pure deterministic market-price formula. It owns the only final rounding step. */
public final class MarketPriceGenerator {
    private static final long MARKET_SEED_SALT = 0x4c697342616d4d6fL;
    private static final long FIRST_RANDOM_SALT = 0x6d61726b65742d31L;
    private static final long SECOND_RANDOM_SALT = 0x6d61726b65742d32L;
    private static final long DIRECTION_RANDOM_SALT = 0x6d61726b65742d33L;
    private static final long MAGNITUDE_RANDOM_SALT = 0x6d61726b65742d34L;
    private static final double UNIT_53 = 0x1.0p-53;
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
        long commodityHash = stableKeyHash(commodity.getVariantIdentity());
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
     * yesterday's saved price and the category's explicit price band.
     * A later catalog reprice leaves its saved predecessor untouched and lets
     * this step walk it back toward the new band instead of rebasing it.
     */
    public static long calculateNextPrice(long marketSeed, long worldDay, MarketCommodity commodity,
                                          long previousPrice, boolean moreStableVolatility) {
        long minimum = minimumPrice(commodity.getBasePrice(), commodity.getCategory());
        long maximum = maximumPrice(commodity.getBasePrice(), commodity.getCategory());
        if (moreStableVolatility) {
            // The legacy independent mode still honors a manual reprice
            // migration. A saved value outside its new band must walk back
            // rather than jumping straight to a newly sampled independent value.
            if (previousPrice < minimum || previousPrice > maximum) {
                long commodityHash = stableKeyHash(commodity.getVariantIdentity());
                double direction = toUnit(mix64(marketSeed ^ mix64(worldDay) ^ commodityHash
                        ^ DIRECTION_RANDOM_SALT));
                double magnitude = toUnit(mix64(marketSeed ^ mix64(worldDay) ^ commodityHash
                        ^ MAGNITUDE_RANDOM_SALT));
                return calculateChainedPrice(commodity.getBasePrice(), commodity.getCategory(), previousPrice,
                        direction, magnitude);
            }
            return calculatePrice(marketSeed, worldDay, commodity);
        }
        long commodityHash = stableKeyHash(commodity.getVariantIdentity());
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
        return clamp(Math.max(1L, Math.round(scaled)), minimumPrice(basePrice, category),
                maximumPrice(basePrice, category));
    }

    /** Pure one-day chained step retained package-visible for deterministic regression checks. */
    static long calculateChainedPrice(long basePrice, CommodityCategory category, long previousPrice,
                                      double direction, double magnitude) {
        if (!isBasePriceSupported(basePrice, category) || previousPrice <= 0L
                || direction < 0.0D || direction >= 1.0D || magnitude < 0.0D || magnitude >= 1.0D) {
            throw new IllegalArgumentException("Invalid chained market price input.");
        }

        long minimum = minimumPrice(basePrice, category);
        long maximum = maximumPrice(basePrice, category);
        boolean previousPriceWithinBand = previousPrice >= minimum && previousPrice <= maximum;
        long oldPrice = previousPrice;
        double deviation = Math.abs((double) oldPrice - (double) basePrice) / (double) basePrice;
        double restoreProbability = restoreProbability(deviation);

        boolean increases;
        if (oldPrice <= minimum) {
            increases = true;
        } else if (oldPrice >= maximum) {
            increases = false;
        } else if (oldPrice < basePrice) {
            increases = direction < restoreProbability;
        } else if (oldPrice > basePrice) {
            increases = direction >= restoreProbability;
        } else {
            increases = direction < 0.5D;
        }

        double step = category.getVolatility() * (0.25D + magnitude * 0.75D);
        double multiplier = increases ? 1.0D + step : 1.0D - step;
        double scaled = oldPrice * multiplier;
        if (scaled > Long.MAX_VALUE - 0.5D) {
            return maximum;
        }
        long rounded = Math.max(1L, Math.round(scaled));
        if (increases && rounded <= oldPrice) {
            rounded = oldPrice == Long.MAX_VALUE ? Long.MAX_VALUE : oldPrice + 1L;
        } else if (!increases && rounded >= oldPrice) {
            rounded = oldPrice <= 1L ? 1L : oldPrice - 1L;
        }
        // A current-band value retains the hard category guard. An old saved
        // price outside a newly adjusted base's band moves by the same daily
        // rule until it naturally re-enters that band; it is never rewritten
        // on load or snapped to the new edge.
        return previousPriceWithinBand ? clamp(rounded, minimum, maximum) : rounded;
    }

    static double restoreProbability(double deviation) {
        if (deviation <= 0.0D) {
            return 0.50D;
        }
        if (deviation <= 0.10D) {
            return 0.55D;
        }
        if (deviation <= 0.25D) {
            return 0.65D;
        }
        if (deviation <= 0.50D) {
            return 0.75D;
        }
        return 0.85D;
    }

    public static long minimumPrice(long basePrice, CommodityCategory category) {
        if (!isBasePriceSupported(basePrice, category)) {
            throw new IllegalArgumentException("Invalid market price input.");
        }
        return Math.max(1L, Math.round(basePrice * category.getMinimumMultiplier()));
    }

    public static long maximumPrice(long basePrice, CommodityCategory category) {
        if (!isBasePriceSupported(basePrice, category)) {
            throw new IllegalArgumentException("Invalid market price input.");
        }
        double scaled = basePrice * category.getMaximumMultiplier();
        if (scaled > Long.MAX_VALUE - 0.5D) {
            throw new IllegalStateException("Frozen market maximum overflows long.");
        }
        return Math.max(1L, Math.round(scaled));
    }

    private static long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    static boolean isBasePriceSupported(long basePrice, CommodityCategory category) {
        if (basePrice <= 0L || category == null) {
            return false;
        }
        return basePrice <= (long) ((Long.MAX_VALUE - 1.0D) / Math.max(1.0D,
                category.getMaximumMultiplier()));
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
