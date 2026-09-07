package lisbam.pastoraleconomy.shipping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pure integer arithmetic for the shipping-box daily settlement. */
public final class ShippingBoxRules {
    public static final int INCOME_PERCENT = 70;

    private ShippingBoxRules() {
    }

    /** Returns -1 when the non-negative unit-price multiplication would overflow. */
    public static long calculateGrossIncome(long unitPrice, int itemCount) {
        if (unitPrice < 0L || itemCount < 0 || (itemCount > 0 && unitPrice > Long.MAX_VALUE / itemCount)) {
            return -1L;
        }
        return unitPrice * itemCount;
    }

    /** Floors the full-box payout to exactly 70% without overflowing a long. */
    public static long calculateDiscountedIncome(long grossIncome) {
        if (grossIncome < 0L) {
            return -1L;
        }
        return grossIncome / 100L * INCOME_PERCENT + grossIncome % 100L * INCOME_PERCENT / 100L;
    }

    /** Returns -1 if the non-negative addition would overflow. */
    public static long addIncome(long current, long addition) {
        if (current < 0L || addition < 0L || addition > Long.MAX_VALUE - current) {
            return -1L;
        }
        return current + addition;
    }

    /**
     * Splits the entire already-discounted value across distinct voucher
     * owners. The one-coin remainders go in UUID order, so the split is
     * deterministic, lossless, and differs by at most one coin.
     */
    public static Map<UUID, Long> splitEvenly(long income, Collection<UUID> recipients) {
        if (income < 0L || recipients == null || recipients.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UUID> ordered = new ArrayList<UUID>();
        for (UUID recipient : recipients) {
            if (recipient != null && !ordered.contains(recipient)) {
                ordered.add(recipient);
            }
        }
        if (ordered.isEmpty()) {
            return Collections.emptyMap();
        }
        Collections.sort(ordered);
        long base = income / ordered.size();
        long remainder = income % ordered.size();
        Map<UUID, Long> result = new LinkedHashMap<UUID, Long>();
        for (int index = 0; index < ordered.size(); index++) {
            result.put(ordered.get(index), Long.valueOf(base + (index < remainder ? 1L : 0L)));
        }
        return Collections.unmodifiableMap(result);
    }
}
