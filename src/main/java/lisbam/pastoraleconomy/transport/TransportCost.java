package lisbam.pastoraleconomy.transport;

import net.minecraft.util.math.BlockPos;

/** Single source for the frozen batch-14 horizontal-distance and fee rules. */
public final class TransportCost {
    public static final long BASE_CONNECTION_FEE = 4000L;
    public static final long MINIMUM_CONNECTION_FEE = 100L;
    public static final long BASE_TRAVEL_FEE = 400L;
    public static final long MINIMUM_TRAVEL_FEE = 50L;

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
        long numerator = BASE_CONNECTION_FEE + 12L * distance;
        long roundedToHundred = ((numerator + 50L) / 100L) * 100L;
        return Math.max(MINIMUM_CONNECTION_FEE, roundedToHundred);
    }

    /** Frozen travel rule: round50(400 + 1.20 * D), using exact integer arithmetic. */
    public static long travelFee(long distance) {
        if (distance < 0L) {
            throw new IllegalArgumentException("Distance cannot be negative.");
        }
        // (400 + 1.20D) / 50 = (4000 + 12D) / 500.
        long numerator = BASE_TRAVEL_FEE * 10L + 12L * distance;
        long rounded = ((numerator + 250L) / 500L) * 50L;
        return Math.max(MINIMUM_TRAVEL_FEE, rounded);
    }
}
