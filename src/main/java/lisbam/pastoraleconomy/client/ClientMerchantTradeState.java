package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.merchant.MerchantTradeSnapshot;

import javax.annotation.Nullable;

/** Transient client-only merchant snapshot cache. */
public final class ClientMerchantTradeState {
    private static MerchantTradeSnapshot snapshot;

    private ClientMerchantTradeState() {
    }

    public static void accept(MerchantTradeSnapshot value) {
        snapshot = value;
    }

    @Nullable
    public static MerchantTradeSnapshot get() {
        return snapshot;
    }

    public static void clear() {
        snapshot = null;
    }
}
