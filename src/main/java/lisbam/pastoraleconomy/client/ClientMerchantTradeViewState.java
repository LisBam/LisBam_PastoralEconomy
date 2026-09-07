package lisbam.pastoraleconomy.client;

/**
 * Client-only display preference shared by all merchant screens in one game
 * session. It deliberately carries no trade, inventory, or economic state.
 */
public final class ClientMerchantTradeViewState {
    public static final int PAGE_SELL = 0;
    public static final int PAGE_BUY = 1;
    public static final int PAGE_EMERALD = 2;

    private static int page;

    private ClientMerchantTradeViewState() {
    }

    public static boolean isBuyPage() {
        return page == PAGE_BUY;
    }

    public static void setBuyPage(boolean value) {
        page = value ? PAGE_BUY : PAGE_SELL;
    }

    public static int getPage() {
        return page;
    }

    public static void setPage(int value) {
        page = value < PAGE_SELL || value > PAGE_EMERALD ? PAGE_SELL : value;
    }
}
