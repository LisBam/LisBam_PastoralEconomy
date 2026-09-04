package lisbam.pastoraleconomy.client;

/**
 * Client-only display preference shared by all merchant screens in one game
 * session. It deliberately carries no trade, inventory, or economic state.
 */
public final class ClientMerchantTradeViewState {
    private static boolean buyPage;

    private ClientMerchantTradeViewState() {
    }

    public static boolean isBuyPage() {
        return buyPage;
    }

    public static void setBuyPage(boolean value) {
        buyPage = value;
    }
}
