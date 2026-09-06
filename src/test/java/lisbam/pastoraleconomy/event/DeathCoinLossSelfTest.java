package lisbam.pastoraleconomy.event;

import java.util.Random;

/** Deterministic boundary checks for percentage and minimum death coin charges. */
public final class DeathCoinLossSelfTest {
    private DeathCoinLossSelfTest() {
    }

    public static void main(String[] args) {
        require(DeathCoinLossRules.rollLoss(100000L, new FixedRandom(0, 0)) == 10000L,
                "the 10 percent lower boundary must apply above the random minimum");
        require(DeathCoinLossRules.rollLoss(100000L, new FixedRandom(20, 1000)) == 30000L,
                "the 30 percent upper boundary must apply above the random minimum");
        require(DeathCoinLossRules.rollLoss(5000L, new FixedRandom(0, 1000)) == 2000L,
                "the inclusive random 1000-2000 minimum must override a smaller percentage");
        require(DeathCoinLossRules.rollLoss(1500L, new FixedRandom(10, 1000)) == 1500L,
                "a player can never lose more coins than they own");
        require(DeathCoinLossRules.rollLoss(0L, new FixedRandom()) == 0L,
                "zero balance must not consume random values or produce a loss");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class FixedRandom extends Random {
        private final int[] values;
        private int index;

        private FixedRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            if (index >= values.length) {
                throw new AssertionError("test random exhausted for bound " + bound);
            }
            int value = values[index++];
            if (value < 0 || value >= bound) {
                throw new AssertionError("test random value " + value + " outside bound " + bound);
            }
            return value;
        }
    }
}
