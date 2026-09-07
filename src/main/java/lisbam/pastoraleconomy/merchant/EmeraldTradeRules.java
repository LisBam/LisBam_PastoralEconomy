package lisbam.pastoraleconomy.merchant;

/** Pure integer calculations shared by the emerald market preview and server settlement. */
public final class EmeraldTradeRules {
    public static final int FEE_PERCENT = 4;

    private EmeraldTradeRules() {
    }

    /** ceil(price * amount * 1.04), or -1 if the base multiplication overflows. */
    public static long calculateBuyCost(long price, int amount) {
        long base = calculateBaseAmount(price, amount);
        if (base < 0L) {
            return -1L;
        }
        long fee = calculateFee(base);
        return fee < 0L || base > Long.MAX_VALUE - fee ? -1L : base + fee;
    }

    /** floor(price * amount * 0.96), or -1 if the base multiplication overflows. */
    public static long calculateSellIncome(long price, int amount) {
        long base = calculateBaseAmount(price, amount);
        if (base < 0L) {
            return -1L;
        }
        long fee = calculateFee(base);
        return fee < 0L ? -1L : base - fee;
    }

    /** The displayed fee is the integer difference between gross and settled amount. */
    public static long calculateFee(long baseAmount) {
        if (baseAmount < 0L) {
            return -1L;
        }
        // ceil(baseAmount * 4 / 100), expressed without a potentially
        // overflowing multiplication and equal to ceil(baseAmount / 25).
        return baseAmount / 25L + (baseAmount % 25L == 0L ? 0L : 1L);
    }

    public static long calculateBaseAmount(long price, int amount) {
        if (price <= 0L || amount <= 0 || price > Long.MAX_VALUE / (long) amount) {
            return -1L;
        }
        return price * (long) amount;
    }

    /** Finds the largest affordable quantity without trusting a client-derived price or total. */
    public static int getMaximumAffordableAmount(long balance, long price, int hardLimit) {
        if (balance < 0L || price <= 0L || hardLimit <= 0) {
            return 0;
        }
        int low = 0;
        int high = hardLimit;
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            long cost = calculateBuyCost(price, middle);
            if (cost >= 0L && cost <= balance) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }
}
