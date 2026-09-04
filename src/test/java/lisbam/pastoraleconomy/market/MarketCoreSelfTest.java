package lisbam.pastoraleconomy.market;

import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;

import java.util.List;

/** Standalone deterministic checks for the fourth-batch market data boundary. */
public final class MarketCoreSelfTest {
    private static final String WHEAT = "lisbam_pastoral_economy:sell/crop/wheat";

    private MarketCoreSelfTest() {
    }

    public static void main(String[] args) {
        Bootstrap.register();
        verifyCatalogAndFormula();
        verifyWorldDataProgressionAndHistory();
        verifySaveReloadRollbackAndMigration();
    }

    private static void verifyCatalogAndFormula() {
        MarketCommodity wheat = MarketCatalog.get(WHEAT);
        require(wheat != null && wheat.getBasePrice() == 5L, "wheat base sell price must stay frozen at five");
        require(MarketCatalog.getHistoryTracked().size() == 14, "exactly fourteen crops must be history tracked");
        require(!MarketCatalog.get("lisbam_pastoral_economy:sell/agriculture/apple").isHistoryTracked(),
                "apple must not have a crop history");

        long expected = MarketPriceGenerator.calculatePrice(71L, 12L, wheat);
        for (int index = 0; index < 100; index++) {
            require(expected == MarketPriceGenerator.calculatePrice(71L, 12L, wheat),
                    "one world/day/commodity must be deterministic for 100 calls");
        }
        require(expected == MarketPriceGenerator.calculatePrice(71L, 12L, wheat), "catalog order cannot affect a price");
        for (CommodityCategory category : CommodityCategory.values()) {
            long low = MarketPriceGenerator.calculatePrice(1L, category, 0.0D, 0.0D);
            long high = MarketPriceGenerator.calculatePrice(1000L, category, 0.999999999999D, 0.999999999999D);
            require(low >= 1L, "all volatility groups must retain the minimum price");
            require(high <= Math.round(1000L * (1.0D + category.getVolatility())),
                    "all volatility groups must stay inside their frozen upper range");
        }
    }

    private static void verifyWorldDataProgressionAndHistory() {
        PastoralWorldData data = new PastoralWorldData();
        data.ensureMarketDay(10L, 12345L);
        long dayTenPrice = price(data, WHEAT);
        require(!data.hasPreviousMarketSnapshot(), "the first market day must not invent yesterday");
        assertHistoryDays(data, 10L, 10L, 1);

        data.ensureMarketDay(10L, 12345L);
        require(dayTenPrice == price(data, WHEAT), "same-day update cannot refreeze a price");
        data.ensureMarketDay(13L, 12345L);
        require(data.getCurrentMarketDay() == 13L, "multi-day jump must reach the actual world day");
        require(data.hasPreviousMarketSnapshot(), "a real adjacent previous snapshot must exist after a forward jump");
        assertHistoryDays(data, 10L, 13L, 4);
        require(data.getPreviousMarketPrice(WHEAT).longValue()
                        == data.getCropHistoryPoint(WHEAT, 12L, 13L).getPrice(),
                "day thirteen trend must compare with day twelve, not day ten");

        data.ensureMarketDay(39L, 12345L);
        List<MarketHistoryPoint> recentThirty = data.getCropHistory(WHEAT, 39L, 30, null);
        require(recentThirty.size() == 30 && recentThirty.get(0).getWorldDay() == 10L
                        && recentThirty.get(29).getWorldDay() == 39L,
                "thirty sequential world days must return the ordered thirty-point window");
        data.ensureMarketDay(45L, 12345L);
        recentThirty = data.getCropHistory(WHEAT, 45L, 30, null);
        require(recentThirty.get(0).getWorldDay() == 16L && recentThirty.get(29).getWorldDay() == 45L,
                "recent history must not delete older actual points");
        List<MarketHistoryPoint> older = data.getCropHistory(WHEAT, 45L, 30, Long.valueOf(16L));
        require(older.size() == 6 && older.get(0).getWorldDay() == 10L && older.get(5).getWorldDay() == 15L,
                "older history must remain available through pagination");
    }

    private static void verifySaveReloadRollbackAndMigration() {
        PastoralWorldData data = new PastoralWorldData();
        data.ensureMarketDay(8L, 99L);
        data.ensureMarketDay(12L, 99L);
        long dayTenPrice = data.getCropHistoryPoint(WHEAT, 10L, 12L).getPrice();
        NBTTagCompound saved = data.writeToNBT(new NBTTagCompound());
        PastoralWorldData restored = new PastoralWorldData();
        restored.readFromNBT(saved);
        restored.ensureMarketDay(12L, 99L);
        require(price(restored, WHEAT) == price(data, WHEAT), "restart must retain the current snapshot");
        restored.ensureMarketDay(10L, 99L);
        require(price(restored, WHEAT) == dayTenPrice, "rollback must reuse the existing day price");
        require(restored.getCropHistory(WHEAT, 10L, 100, null).size() == 3,
                "rollback must hide later history without deleting it");
        restored.ensureMarketDay(12L, 99L);
        require(restored.getCropHistory(WHEAT, 12L, 100, null).size() == 5,
                "returning to a processed day must not duplicate history");

        NBTTagCompound v2 = new NBTTagCompound();
        v2.setInteger("dataVersion", 2);
        v2.setBoolean("firstInitializationCompleted", true);
        v2.setTag("farmHarassment", new net.minecraft.nbt.NBTTagList());
        PastoralWorldData migrated = new PastoralWorldData();
        migrated.readFromNBT(v2);
        NBTTagCompound migratedNbt = migrated.writeToNBT(new NBTTagCompound());
        require(migratedNbt.getInteger("dataVersion") == PastoralWorldData.DATA_VERSION,
                "legacy world data must write at the current schema version");
        require(migratedNbt.getBoolean("firstInitializationCompleted"), "v2 initialization marker must survive migration");
        require(!migratedNbt.getCompoundTag("market").getBoolean("initialized"), "v2 migration must not fabricate market data");
    }

    private static void assertHistoryDays(PastoralWorldData data, long first, long last, int expectedCount) {
        List<MarketHistoryPoint> points = data.getCropHistory(WHEAT, last, 100, null);
        require(points.size() == expectedCount && points.get(0).getWorldDay() == first
                        && points.get(points.size() - 1).getWorldDay() == last,
                "history must contain each actual market day once");
    }

    private static long price(PastoralWorldData data, String key) {
        Long price = data.getCurrentMarketPrice(key);
        if (price == null) {
            throw new AssertionError("missing current price for " + key);
        }
        return price.longValue();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
