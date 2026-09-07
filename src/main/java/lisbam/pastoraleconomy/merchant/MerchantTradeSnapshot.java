package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.market.EmeraldMarketPriceGenerator;

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
    private final long emeraldCurrentPrice;
    private final long emeraldPreviousPrice;
    private final int emeraldHoldings;
    private final int emeraldMaxBuy;
    private final int emeraldMaxSell;
    private final List<MerchantTradeOfferView> sellOffers;
    private final List<MerchantTradeOfferView> buyOffers;

    public MerchantTradeSnapshot(UUID merchantId, int windowId, long worldDay, long balance,
                                 List<MerchantTradeOfferView> sellOffers,
                                 List<MerchantTradeOfferView> buyOffers) {
        this(merchantId, windowId, worldDay, balance, EmeraldMarketPriceGenerator.INITIAL_PRICE,
                EmeraldMarketPriceGenerator.INITIAL_PRICE, 0, 0, 0, sellOffers, buyOffers);
    }

    public MerchantTradeSnapshot(UUID merchantId, int windowId, long worldDay, long balance,
                                 long emeraldCurrentPrice, long emeraldPreviousPrice, int emeraldHoldings,
                                 int emeraldMaxBuy, int emeraldMaxSell, List<MerchantTradeOfferView> sellOffers,
                                 List<MerchantTradeOfferView> buyOffers) {
        if (merchantId == null || worldDay < 0L || balance < 0L || sellOffers == null || buyOffers == null
                || sellOffers.size() != SELL_COUNT || buyOffers.size() != BUY_COUNT
                || emeraldCurrentPrice < EmeraldMarketPriceGenerator.MINIMUM_PRICE
                || emeraldCurrentPrice > EmeraldMarketPriceGenerator.MAXIMUM_PRICE
                || emeraldPreviousPrice < EmeraldMarketPriceGenerator.MINIMUM_PRICE
                || emeraldPreviousPrice > EmeraldMarketPriceGenerator.MAXIMUM_PRICE
                || emeraldHoldings < 0 || emeraldMaxBuy < 0 || emeraldMaxSell < 0) {
            throw new IllegalArgumentException("Invalid merchant trade snapshot.");
        }
        this.merchantId = merchantId;
        this.windowId = windowId;
        this.worldDay = worldDay;
        this.balance = balance;
        this.emeraldCurrentPrice = emeraldCurrentPrice;
        this.emeraldPreviousPrice = emeraldPreviousPrice;
        this.emeraldHoldings = emeraldHoldings;
        this.emeraldMaxBuy = emeraldMaxBuy;
        this.emeraldMaxSell = emeraldMaxSell;
        this.sellOffers = Collections.unmodifiableList(new ArrayList<MerchantTradeOfferView>(sellOffers));
        this.buyOffers = Collections.unmodifiableList(new ArrayList<MerchantTradeOfferView>(buyOffers));
    }

    public UUID getMerchantId() { return merchantId; }
    public int getWindowId() { return windowId; }
    public long getWorldDay() { return worldDay; }
    public long getBalance() { return balance; }
    public long getEmeraldCurrentPrice() { return emeraldCurrentPrice; }
    public long getEmeraldPreviousPrice() { return emeraldPreviousPrice; }
    public int getEmeraldHoldings() { return emeraldHoldings; }
    public int getEmeraldMaxBuy() { return emeraldMaxBuy; }
    public int getEmeraldMaxSell() { return emeraldMaxSell; }
    public List<MerchantTradeOfferView> getSellOffers() { return sellOffers; }
    public List<MerchantTradeOfferView> getBuyOffers() { return buyOffers; }
}
