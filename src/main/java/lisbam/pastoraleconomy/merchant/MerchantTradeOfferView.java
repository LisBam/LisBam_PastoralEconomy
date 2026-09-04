package lisbam.pastoraleconomy.merchant;

/** Immutable, bounded display view; it contains no authoritative inventory or entity references. */
public final class MerchantTradeOfferView {
    private final boolean enabled;
    private final String catalogKey;
    private final int groupSize;
    private final long currentPrice;
    private final long previousPrice;
    private final boolean hasPreviousPrice;
    private final int remainingGroups;
    private final int enchantmentLevel;

    public MerchantTradeOfferView(boolean enabled, String catalogKey, int groupSize, long currentPrice,
                                  Long previousPrice, int remainingGroups) {
        this(enabled, catalogKey, groupSize, currentPrice, previousPrice, remainingGroups, 0);
    }

    public MerchantTradeOfferView(boolean enabled, String catalogKey, int groupSize, long currentPrice,
                                  Long previousPrice, int remainingGroups, int enchantmentLevel) {
        this.enabled = enabled;
        this.catalogKey = catalogKey == null ? "" : catalogKey;
        this.groupSize = groupSize;
        this.currentPrice = currentPrice;
        this.hasPreviousPrice = previousPrice != null;
        this.previousPrice = previousPrice == null ? 0L : previousPrice.longValue();
        this.remainingGroups = remainingGroups;
        this.enchantmentLevel = Math.max(0, enchantmentLevel);
    }

    public boolean isEnabled() { return enabled; }
    public String getCatalogKey() { return catalogKey; }
    public int getGroupSize() { return groupSize; }
    public long getCurrentPrice() { return currentPrice; }
    public boolean hasPreviousPrice() { return hasPreviousPrice; }
    public long getPreviousPrice() { return previousPrice; }
    public int getRemainingGroups() { return remainingGroups; }
    public int getEnchantmentLevel() { return enchantmentLevel; }
}
