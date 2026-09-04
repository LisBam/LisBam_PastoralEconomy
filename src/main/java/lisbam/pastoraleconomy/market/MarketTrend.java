package lisbam.pastoraleconomy.market;

/** One shared comparison rule for current prices and historical points. */
public enum MarketTrend {
    UP("↑"),
    DOWN("↓"),
    FLAT("—");

    private final String symbol;

    MarketTrend(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static MarketTrend compare(long currentPrice, long previousPrice) {
        if (currentPrice > previousPrice) {
            return UP;
        }
        if (currentPrice < previousPrice) {
            return DOWN;
        }
        return FLAT;
    }
}
