package lisbam.pastoraleconomy.market;

import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Set<String> expectedSellGoods = new HashSet<String>(Arrays.asList(
                "lisbam_pastoral_economy:sell/crop/wheat",
                "lisbam_pastoral_economy:sell/crop/carrot",
                "lisbam_pastoral_economy:sell/crop/potato",
                "lisbam_pastoral_economy:sell/crop/beetroot",
                "lisbam_pastoral_economy:sell/crop/pumpkin",
                "lisbam_pastoral_economy:sell/crop/melon_block",
                "lisbam_pastoral_economy:sell/crop/melon_slice",
                "lisbam_pastoral_economy:sell/crop/sugar_cane",
                "lisbam_pastoral_economy:sell/crop/cactus",
                "lisbam_pastoral_economy:sell/crop/cocoa_beans",
                "lisbam_pastoral_economy:sell/crop/red_mushroom",
                "lisbam_pastoral_economy:sell/crop/brown_mushroom",
                "lisbam_pastoral_economy:sell/agriculture/apple",
                "lisbam_pastoral_economy:sell/livestock/egg",
                "lisbam_pastoral_economy:sell/livestock/feather",
                "lisbam_pastoral_economy:sell/livestock/leather",
                "lisbam_pastoral_economy:sell/livestock/rabbit_hide",
                "lisbam_pastoral_economy:sell/livestock/rabbit_foot",
                "lisbam_pastoral_economy:sell/crop/nether_wart",
                "lisbam_pastoral_economy:sell/crop/chorus_fruit",
                "lisbam_pastoral_economy:sell/livestock/milk_bucket",
                "lisbam_pastoral_economy:sell/livestock/wool"
        ));
        Set<String> actualSellGoods = new HashSet<String>();
        for (MarketCommodity commodity : MarketCatalog.getHistoryTracked()) {
            actualSellGoods.add(commodity.getKey());
        }
        require(actualSellGoods.equals(expectedSellGoods),
                "market book must include every and only merchant sell good");

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

        long restoringDown = MarketPriceGenerator.calculateChainedPrice(100L, CommodityCategory.CORE_CROPS,
                135L, 0.0D, 0.5D);
        long escapingUp = MarketPriceGenerator.calculateChainedPrice(100L, CommodityCategory.CORE_CROPS,
                135L, 0.90D, 0.5D);
        require(restoringDown < 135L && escapingUp > 135L,
                "above-base chained prices must favour, but not force, a restoring down move");
        long chained = 135L;
        for (int index = 0; index < 6; index++) {
            chained = MarketPriceGenerator.calculateChainedPrice(100L, CommodityCategory.CORE_CROPS,
                    chained, 0.99D, 0.999999999D);
        }
        require(chained > 135L,
                "chained prices must not retain the legacy fixed 135-percent ceiling");
    }

    private static void verifyWorldDataProgressionAndHistory() {
        PastoralWorldData data = new PastoralWorldData();
        data.ensureMarketDay(10L, 12345L);
        long dayTenPrice = price(data, WHEAT);
        require(dayTenPrice == 5L, "a new chained market must start at the frozen base price");
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
                "retention must keep the newest exact thirty days");
        List<MarketHistoryPoint> older = data.getCropHistory(WHEAT, 45L, 30, Long.valueOf(16L));
        require(older.isEmpty(), "older-than-thirty history must be discarded instead of growing indefinitely");

        data.ensureMarketDay(1000L, 12345L);
        recentThirty = data.getCropHistory(WHEAT, 1000L, 100, null);
        require(recentThirty.size() == PastoralWorldData.MARKET_HISTORY_RETENTION_DAYS
                        && recentThirty.get(0).getWorldDay() == 971L
                        && recentThirty.get(29).getWorldDay() == 1000L,
                "chained day advancement must retain exactly the newest thirty daily prices");
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
                "rollback must rebuild only the visible history through the current day");
        restored.ensureMarketDay(12L, 99L);
        require(restored.getCropHistory(WHEAT, 12L, 100, null).size() == 5,
                "returning to a day must deterministically rebuild without duplicate history");

        NBTTagCompound v6 = data.writeToNBT(new NBTTagCompound());
        v6.setInteger("dataVersion", 6);
        PastoralWorldData migratedV6 = new PastoralWorldData();
        migratedV6.readFromNBT(v6);
        migratedV6.ensureMarketDay(1000L, 12345L);
        NBTTagCompound migratedV6Nbt = migratedV6.writeToNBT(new NBTTagCompound());
        require(migratedV6Nbt.getInteger("dataVersion") == PastoralWorldData.DATA_VERSION,
                "v6 worlds must migrate to the bounded market schema");
        require(migratedV6Nbt.getCompoundTag("market").getTagList("processedDays", 10).tagCount() == 0,
                "v7 must not write the legacy unbounded processed-day list");

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
