package lisbam.pastoraleconomy.client;

/** Client-only, non-persistent UI cache. It is never used for authority. */
public final class ClientPlayerState {
    private static long coins;
    private static boolean hasValidCoinSync;

    private ClientPlayerState() {
    }

    public static void acceptCoins(long synchronizedCoins) {
        coins = synchronizedCoins < 0L ? 0L : synchronizedCoins;
        hasValidCoinSync = true;
    }

    public static void clear() {
        coins = 0L;
        hasValidCoinSync = false;
    }

    public static boolean hasValidCoinSync() {
        return hasValidCoinSync;
    }

    public static long getCoins() {
        return coins;
    }
}
