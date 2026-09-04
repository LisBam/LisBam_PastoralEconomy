package lisbam.pastoraleconomy.merchant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Bounded client snapshot for one active merchant GUI session. */
public final class MerchantTradeSnapshot {
    public static final int SELL_COUNT = DailyOfferState.SELL_OFFER_COUNT;
    public static final int BUY_COUNT = DailyOfferState.BUY_OFFER_COUNT;

    private final UUID merchantId;
    private final int windowId;
    private final long worldDay;
    private final long balance;
    private final List<MerchantTradeOfferView> sellOffers;
    private final List<MerchantTradeOfferView> buyOffers;

    public MerchantTradeSnapshot(UUID merchantId, int windowId, long worldDay, long balance,
                                 List<MerchantTradeOfferView> sellOffers,
                                 List<MerchantTradeOfferView> buyOffers) {
        if (merchantId == null || worldDay < 0L || balance < 0L || sellOffers == null || buyOffers == null
                || sellOffers.size() != SELL_COUNT || buyOffers.size() != BUY_COUNT) {
            throw new IllegalArgumentException("Invalid merchant trade snapshot.");
        }
        this.merchantId = merchantId;
        this.windowId = windowId;
        this.worldDay = worldDay;
        this.balance = balance;
        this.sellOffers = Collections.unmodifiableList(new ArrayList<MerchantTradeOfferView>(sellOffers));
        this.buyOffers = Collections.unmodifiableList(new ArrayList<MerchantTradeOfferView>(buyOffers));
    }

    public UUID getMerchantId() { return merchantId; }
    public int getWindowId() { return windowId; }
    public long getWorldDay() { return worldDay; }
    public long getBalance() { return balance; }
    public List<MerchantTradeOfferView> getSellOffers() { return sellOffers; }
    public List<MerchantTradeOfferView> getBuyOffers() { return buyOffers; }
}
