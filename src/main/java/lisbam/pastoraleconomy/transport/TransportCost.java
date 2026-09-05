package lisbam.pastoraleconomy.transport;

import net.minecraft.util.math.BlockPos;

/** Single source for the frozen batch-14 horizontal-distance and fee rules. */
public final class TransportCost {
    public static final long BASE_CONNECTION_FEE = 400L;
    public static final long MINIMUM_CONNECTION_FEE = 10L;
    public static final long BASE_TRAVEL_FEE = 160L;
    public static final long MINIMUM_TRAVEL_FEE = 20L;

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
        // round10(400 + 1.20D), preserving the prior fee curve at one tenth.
        long numerator = BASE_CONNECTION_FEE * 10L + 12L * distance;
        long roundedToTen = ((numerator + 50L) / 100L) * 10L;
        return Math.max(MINIMUM_CONNECTION_FEE, roundedToTen);
    }

    /** Frozen travel rule: round20(160 + 0.48 * D), exactly 400% of the former travel curve. */
    public static long travelFee(long distance) {
        if (distance < 0L) {
            throw new IllegalArgumentException("Distance cannot be negative.");
        }
        // (160 + 0.48D) / 20 = (16000 + 48D) / 2000.
        long numerator = BASE_TRAVEL_FEE * 100L + 48L * distance;
        long rounded = ((numerator + 1000L) / 2000L) * 20L;
        return Math.max(MINIMUM_TRAVEL_FEE, rounded);
    }
}
