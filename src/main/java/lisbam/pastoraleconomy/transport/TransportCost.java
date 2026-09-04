package lisbam.pastoraleconomy.transport;

import net.minecraft.util.math.BlockPos;

/** Single source for the frozen batch-14 horizontal-distance and fee rules. */
public final class TransportCost {
    public static final long BASE_CONNECTION_FEE = 400L;
    public static final long MINIMUM_CONNECTION_FEE = 10L;
    public static final long BASE_TRAVEL_FEE = 40L;
    public static final long MINIMUM_TRAVEL_FEE = 5L;

    private TransportCost() {
    }

    public static long horizontalDistance(BlockPos first, BlockPos second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("Both station positions are required.");
        }
        long deltaX = (long) first.getX() - (long) second.getX();
        long deltaZ = (long) first.getZ() - (long) second.getZ();
        long squaredDistance = deltaX * deltaX + deltaZ * deltaZ;
        long estimate = (long) Math.sqrt((double) squaredDistance);
        // Correct any floating-point edge error without risking another square overflow.
        while (estimate + 1L > 0L && estimate + 1L <= squaredDistance / (estimate + 1L)) {
            estimate++;
        }
        while (estimate > 0L && estimate > squaredDistance / estimate) {
            estimate--;
        }
        return estimate;
    }

    public static long connectionFee(long distance) {
        if (distance < 0L) {
            throw new IllegalArgumentException("Distance cannot be negative.");
        }
        long roundedToTen = Math.round((BASE_CONNECTION_FEE + 1.20D * (double) distance) / 10.0D) * 10L;
        return Math.max(MINIMUM_CONNECTION_FEE, roundedToTen);
    }

    /** Frozen travel rule: round5(40 + 0.12 * D), using exact integer arithmetic. */
    public static long travelFee(long distance) {
        if (distance < 0L) {
            throw new IllegalArgumentException("Distance cannot be negative.");
        }
        // (40 + 0.12D) / 5 = (1000 + 3D) / 125.  Add half the
        // denominator for deterministic non-negative half-up rounding.
        long numerator = BASE_TRAVEL_FEE * 25L + 3L * distance;
        long rounded = ((numerator + 62L) / 125L) * 5L;
        return Math.max(MINIMUM_TRAVEL_FEE, rounded);
    }
}
