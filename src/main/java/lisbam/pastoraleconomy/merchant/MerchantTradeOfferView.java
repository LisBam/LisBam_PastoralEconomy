package lisbam.pastoraleconomy.merchant;

/** Immutable, bounded display view; it contains no authoritative inventory or entity references. */
public final class MerchantTradeOfferView {
    private final boolean enabled;
    private final String catalogKey;
    private final int itemStackLimit;
    private final long currentPrice;
    private final long previousPrice;
    private final boolean hasPreviousPrice;
    /** Buy offer stock, or voucher-linked chest stock for a sell offer. */
    private final int remainingItems;
    private final int enchantmentLevel;

    public MerchantTradeOfferView(boolean enabled, String catalogKey, int itemStackLimit, long currentPrice,
                                  Long previousPrice, int remainingItems) {
        this(enabled, catalogKey, itemStackLimit, currentPrice, previousPrice, remainingItems, 0);
    }

    public MerchantTradeOfferView(boolean enabled, String catalogKey, int itemStackLimit, long currentPrice,
                                  Long previousPrice, int remainingItems, int enchantmentLevel) {
        this.enabled = enabled;
        this.catalogKey = catalogKey == null ? "" : catalogKey;
        this.itemStackLimit = itemStackLimit;
        this.currentPrice = currentPrice;
        this.hasPreviousPrice = previousPrice != null;
        this.previousPrice = previousPrice == null ? 0L : previousPrice.longValue();
        this.remainingItems = remainingItems;
        this.enchantmentLevel = Math.max(0, enchantmentLevel);
    }

    public boolean isEnabled() { return enabled; }
    public String getCatalogKey() { return catalogKey; }
    public int getItemStackLimit() { return itemStackLimit; }
    public long getCurrentPrice() { return currentPrice; }
    public boolean hasPreviousPrice() { return hasPreviousPrice; }
    public long getPreviousPrice() { return previousPrice; }
    public int getRemainingItems() { return remainingItems; }
    public int getEnchantmentLevel() { return enchantmentLevel; }
}
