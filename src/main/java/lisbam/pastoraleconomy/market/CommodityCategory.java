package lisbam.pastoraleconomy.market;

/** The eight frozen market volatility groups. */
public enum CommodityCategory {
    CORE_CROPS(0.35D, 0.50D, 2.00D),
    SECONDARY_AGRICULTURE_LIVESTOCK(0.30D, 0.55D, 1.80D),
    SEEDS_AND_AGRICULTURAL_SUPPLIES(0.20D, 0.65D, 1.50D),
    BASIC_BUILDING_MATERIALS(0.15D, 0.70D, 1.40D),
    MINERALS_REDSTONE_COMMON_DROPS(0.18D, 0.65D, 1.50D),
    ADVANCED_NETHER_END_RESOURCES(0.18D, 0.70D, 1.45D),
    RARE_GOODS(0.12D, 0.75D, 1.35D),
    TREASURES_COLLECTIBLES(0.08D, 0.80D, 1.25D);

    private final double volatility;
    private final double minimumMultiplier;
    private final double maximumMultiplier;

    CommodityCategory(double volatility, double minimumMultiplier, double maximumMultiplier) {
        this.volatility = volatility;
        this.minimumMultiplier = minimumMultiplier;
        this.maximumMultiplier = maximumMultiplier;
    }

    public double getVolatility() {
        return volatility;
    }

    public double getMinimumMultiplier() {
        return minimumMultiplier;
    }

    public double getMaximumMultiplier() {
        return maximumMultiplier;
    }
}
