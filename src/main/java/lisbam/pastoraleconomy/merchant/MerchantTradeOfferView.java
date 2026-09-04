package lisbam.pastoraleconomy.merchant;

/** Immutable, bounded display view; it contains no authoritative inventory or entity references. */
public final class MerchantTradeOfferView {
    private final boolean enabled;
    private final String catalogKey;
    private final int bundleSize;
    private final long currentPrice;
    private final long previousPrice;
    private final boolean hasPreviousPrice;
    private final int remainingBundles;
    private final int enchantmentLevel;

    public MerchantTradeOfferView(boolean enabled, String catalogKey, int bundleSize, long currentPrice,
                                  Long previousPrice, int remainingBundles) {
        this(enabled, catalogKey, bundleSize, currentPrice, previousPrice, remainingBundles, 0);
    }

    public MerchantTradeOfferView(boolean enabled, String catalogKey, int bundleSize, long currentPrice,
                                  Long previousPrice, int remainingBundles, int enchantmentLevel) {
        this.enabled = enabled;
        this.catalogKey = catalogKey == null ? "" : catalogKey;
        this.bundleSize = bundleSize;
        this.currentPrice = currentPrice;
        this.hasPreviousPrice = previousPrice != null;
        this.previousPrice = previousPrice == null ? 0L : previousPrice.longValue();
        this.remainingBundles = remainingBundles;
        this.enchantmentLevel = Math.max(0, enchantmentLevel);
    }

    public boolean isEnabled() { return enabled; }
    public String getCatalogKey() { return catalogKey; }
    public int getBundleSize() { return bundleSize; }
    public long getCurrentPrice() { return currentPrice; }
    public boolean hasPreviousPrice() { return hasPreviousPrice; }
    public long getPreviousPrice() { return previousPrice; }
    public int getRemainingBundles() { return remainingBundles; }
    public int getEnchantmentLevel() { return enchantmentLevel; }
}
