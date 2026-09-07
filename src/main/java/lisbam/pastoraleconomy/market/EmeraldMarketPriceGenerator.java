package lisbam.pastoraleconomy.market;

import java.util.Random;

/**
 * Deterministic high-volatility emerald market formula.  It deliberately has
 * no restoring bias: only the hard 300--3000 coin range constrains a day.
 */
public final class EmeraldMarketPriceGenerator {
    public static final long INITIAL_PRICE = 1000L;
    public static final long MINIMUM_PRICE = 300L;
    public static final long MAXIMUM_PRICE = 3000L;

    private static final long RANDOM_SALT = 0x456d6572616c644dL;

    private EmeraldMarketPriceGenerator() {
    }

    /** Returns the deterministic next daily price for the shared market seed. */
    public static long calculateNextPrice(long marketSeed, long worldDay, long previousPrice) {
        if (worldDay < 0L || previousPrice < MINIMUM_PRICE || previousPrice > MAXIMUM_PRICE) {
            throw new IllegalArgumentException("Invalid emerald market input.");
        }
        long seed = mix64(marketSeed ^ Long.rotateLeft(worldDay, 19) ^ RANDOM_SALT);
        return calculateNextPrice(previousPrice, new Random(seed));
    }

    /** Package-visible deterministic seam for the market regression self-test. */
    static long calculateNextPrice(long previousPrice, Random random) {
        if (previousPrice < MINIMUM_PRICE || previousPrice > MAXIMUM_PRICE || random == null) {
            throw new IllegalArgumentException("Invalid emerald market input.");
        }
        int changePercent = nextChangePercent(random);
        long scaled = Math.round(previousPrice * (100.0D + changePercent) / 100.0D);
        return Math.max(MINIMUM_PRICE, Math.min(MAXIMUM_PRICE, scaled));
    }

    /**
     * Selects exactly the frozen daily distribution: 25/45/20/8/2 percent.
     * Ranges are inclusive so their documented endpoints are attainable.
     */
    static int nextChangePercent(Random random) {
        int roll = random.nextInt(100);
        if (roll < 25) {
            return between(random, -5, 5);
        }
        if (roll < 70) {
            return between(random, -15, 15);
        }
        if (roll < 90) {
            return between(random, -30, 30);
        }
        if (roll < 98) {
            return between(random, -50, 60);
        }
        return between(random, -70, 100);
    }

    private static int between(Random random, int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
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
