package lisbam.pastoraleconomy.market;

/** The eight frozen market volatility groups. */
public enum CommodityCategory {
    CORE_CROPS(0.35D),
    SECONDARY_AGRICULTURE_LIVESTOCK(0.30D),
    SEEDS_AND_AGRICULTURAL_SUPPLIES(0.20D),
    BASIC_BUILDING_MATERIALS(0.15D),
    MINERALS_REDSTONE_COMMON_DROPS(0.18D),
    ADVANCED_NETHER_END_RESOURCES(0.18D),
    RARE_GOODS(0.12D),
    TREASURES_COLLECTIBLES(0.08D);

    private final double volatility;

    CommodityCategory(double volatility) {
        this.volatility = volatility;
    }

    public double getVolatility() {
        return volatility;
    }
}
