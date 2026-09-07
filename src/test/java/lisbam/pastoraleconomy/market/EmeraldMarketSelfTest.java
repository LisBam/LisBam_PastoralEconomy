package lisbam.pastoraleconomy.market;

import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;

import java.util.List;
import java.util.Random;

/** Deterministic regression checks for the independent persisted emerald market. */
public final class EmeraldMarketSelfTest {
    private EmeraldMarketSelfTest() {
    }

    public static void main(String[] args) {
        Bootstrap.register();
        verifyDailyDistributionBoundaries();
        verifyHardPriceBounds();
        verifyWorldDataInitializationAndMigration();
        System.out.println("emeraldMarketSelfTest PASS");
    }

    private static void verifyDailyDistributionBoundaries() {
        check(EmeraldMarketPriceGenerator.nextChangePercent(new FixedRandom(0, 0)) == -5, "flat lower boundary");
        check(EmeraldMarketPriceGenerator.nextChangePercent(new FixedRandom(24, 10)) == 5, "flat upper boundary");
        check(EmeraldMarketPriceGenerator.nextChangePercent(new FixedRandom(25, 0)) == -15, "normal lower boundary");
        check(EmeraldMarketPriceGenerator.nextChangePercent(new FixedRandom(69, 30)) == 15, "normal upper boundary");
        check(EmeraldMarketPriceGenerator.nextChangePercent(new FixedRandom(70, 0)) == -30, "high lower boundary");
        check(EmeraldMarketPriceGenerator.nextChangePercent(new FixedRandom(89, 60)) == 30, "high upper boundary");
        check(EmeraldMarketPriceGenerator.nextChangePercent(new FixedRandom(90, 0)) == -50, "surge lower boundary");
        check(EmeraldMarketPriceGenerator.nextChangePercent(new FixedRandom(97, 110)) == 60, "surge upper boundary");
        check(EmeraldMarketPriceGenerator.nextChangePercent(new FixedRandom(98, 0)) == -70, "black swan lower boundary");
        check(EmeraldMarketPriceGenerator.nextChangePercent(new FixedRandom(99, 170)) == 100, "black swan upper boundary");
    }

    private static void verifyHardPriceBounds() {
        check(EmeraldMarketPriceGenerator.calculateNextPrice(300L, new FixedRandom(98, 0)) == 300L,
                "minimum clamps a negative black-swan move");
        check(EmeraldMarketPriceGenerator.calculateNextPrice(3000L, new FixedRandom(99, 170)) == 3000L,
                "maximum clamps a positive black-swan move");
        check(EmeraldMarketPriceGenerator.calculateNextPrice(1234L, 45L, 1000L)
                        == EmeraldMarketPriceGenerator.calculateNextPrice(1234L, 45L, 1000L),
                "the same market seed and world day are deterministic");
    }

    private static void verifyWorldDataInitializationAndMigration() {
        PastoralWorldData data = new PastoralWorldData();
        data.ensureMarketDay(40L, 98765L);
        check(data.getEmeraldCurrentPrice() == 1000L && data.getEmeraldPreviousPrice() == 1000L
                        && data.getEmeraldLastUpdateDay() == 40L,
                "a new world initializes emerald price once without a same-day roll");
        List<MarketHistoryPoint> history = data.getEmeraldHistory(30, null);
        check(history.size() == 1 && history.get(0).getWorldDay() == 40L
                        && history.get(0).getPrice() == 1000L && !history.get(0).hasPreviousPoint(),
                "the first emerald price must become the first bounded chart point");
        data.ensureMarketDay(40L, 98765L);
        check(data.getEmeraldCurrentPrice() == 1000L && data.getEmeraldLastUpdateDay() == 40L,
                "reopening the same day cannot reroll emerald price");
        data.ensureMarketDay(42L, 98765L);
        check(data.getEmeraldLastUpdateDay() == 42L
                        && data.getEmeraldCurrentPrice() >= EmeraldMarketPriceGenerator.MINIMUM_PRICE
                        && data.getEmeraldCurrentPrice() <= EmeraldMarketPriceGenerator.MAXIMUM_PRICE
                        && data.getEmeraldPreviousPrice() >= EmeraldMarketPriceGenerator.MINIMUM_PRICE
                        && data.getEmeraldPreviousPrice() <= EmeraldMarketPriceGenerator.MAXIMUM_PRICE,
                "skipped days advance one bounded emerald price step per world day");
        history = data.getEmeraldHistory(30, null);
        check(history.size() == 3 && history.get(0).getWorldDay() == 40L
                        && history.get(2).getWorldDay() == 42L
                        && history.get(2).getPrice() == data.getEmeraldCurrentPrice()
                        && history.get(1).hasPreviousPoint() && history.get(1).getPreviousPrice().longValue() == 1000L,
                "each skipped emerald day must append a sequential chart point with its actual prior price");

        NBTTagCompound saved = data.writeToNBT(new NBTTagCompound());
        PastoralWorldData restored = new PastoralWorldData();
        restored.readFromNBT(saved);
        restored.ensureMarketDay(42L, 98765L);
        check(restored.getEmeraldHistory(30, null).size() == 3
                        && restored.getEmeraldHistory(30, null).get(2).getPrice() == data.getEmeraldCurrentPrice(),
                "the retained emerald chart must survive a save reload without a reroll");

        NBTTagCompound legacy = data.writeToNBT(new NBTTagCompound());
        NBTTagCompound market = legacy.getCompoundTag("market");
        market.removeTag("emeraldInitialized");
        market.removeTag("emeraldCurrentPrice");
        market.removeTag("emeraldPreviousPrice");
        market.removeTag("emeraldLastUpdateDay");
        market.removeTag("emeraldHistory");
        legacy.setInteger("dataVersion", 8);
        PastoralWorldData migrated = new PastoralWorldData();
        migrated.readFromNBT(legacy);
        migrated.ensureMarketDay(42L, 98765L);
        check(migrated.getEmeraldCurrentPrice() == 1000L && migrated.getEmeraldPreviousPrice() == 1000L
                        && migrated.getEmeraldLastUpdateDay() == 42L,
                "an old world without emerald fields initializes at 1000 without an immediate roll");
        check(migrated.getEmeraldHistory(30, null).size() == 1
                        && migrated.getEmeraldHistory(30, null).get(0).getWorldDay() == 42L,
                "an old world starts the new chart at its truthful first visible price");
    }

    private static void check(boolean condition, String message) {
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
            if (index >= values.length || values[index] < 0 || values[index] >= bound) {
                throw new AssertionError("fixed random value outside requested bound");
            }
            return values[index++];
        }
    }
}
