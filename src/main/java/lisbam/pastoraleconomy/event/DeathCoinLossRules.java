package lisbam.pastoraleconomy.event;

import java.util.Random;

/** Pure server-side rule for the coin charge applied once to each real player death. */
final class DeathCoinLossRules {
    private static final int MIN_PERCENT = 10;
    private static final int MAX_PERCENT = 30;
    private static final long MINIMUM_LOSS = 1000L;
    private static final long MAXIMUM_MINIMUM_LOSS = 2000L;

    private DeathCoinLossRules() {
    }

    static long rollLoss(long balance, Random random) {
        if (balance <= 0L) {
            return 0L;
        }
        if (random == null) {
            throw new IllegalArgumentException("A server random source is required for death coin loss.");
        }
        int percent = MIN_PERCENT + random.nextInt(MAX_PERCENT - MIN_PERCENT + 1);
        long percentageLoss = roundedPercent(balance, percent);
        long minimumLoss = MINIMUM_LOSS + random.nextInt((int) (MAXIMUM_MINIMUM_LOSS - MINIMUM_LOSS + 1L));
        long requestedLoss = Math.max(percentageLoss, minimumLoss);
        return Math.min(balance, requestedLoss);
    }

    private static long roundedPercent(long balance, int percent) {
        long wholeHundreds = balance / 100L;
        long remainder = balance % 100L;
        return wholeHundreds * percent + (remainder * percent + 50L) / 100L;
    }
}
