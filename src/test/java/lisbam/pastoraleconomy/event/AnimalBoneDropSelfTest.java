package lisbam.pastoraleconomy.event;

import java.util.Random;

/** Deterministic boundary checks for animal-bone probabilities and Looting bonuses. */
public final class AnimalBoneDropSelfTest {
    private AnimalBoneDropSelfTest() {
    }

    public static void main(String[] args) {
        require(AnimalBoneDropRules.rollLargeAnimalBaseCount(new FixedRandom(0)) == 2,
                "large animals roll two bones below 20");
        require(AnimalBoneDropRules.rollLargeAnimalBaseCount(new FixedRandom(19)) == 2,
                "large animals roll two bones at 19");
        require(AnimalBoneDropRules.rollLargeAnimalBaseCount(new FixedRandom(20)) == 1,
                "large animals roll one bone at 20");
        require(AnimalBoneDropRules.rollLargeAnimalBaseCount(new FixedRandom(69)) == 1,
                "large animals roll one bone at 69");
        require(AnimalBoneDropRules.rollLargeAnimalBaseCount(new FixedRandom(70)) == 0,
                "large animals fail at 70");
        require(AnimalBoneDropRules.rollSmallAnimalBaseCount(new FixedRandom(24)) == 1,
                "small animals roll one bone below 25");
        require(AnimalBoneDropRules.rollSmallAnimalBaseCount(new FixedRandom(25)) == 0,
                "small animals fail at 25");
        require(AnimalBoneDropRules.applyLootingBonus(2, 3, new FixedRandom(0)) == 2,
                "Looting may add zero bones");
        require(AnimalBoneDropRules.applyLootingBonus(1, 3, new FixedRandom(3)) == 4,
                "Looting adds at most its level");
        require(AnimalBoneDropRules.applyLootingBonus(0, 3, new FixedRandom()) == 0,
                "Looting does not turn a failed base roll into a drop");
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
