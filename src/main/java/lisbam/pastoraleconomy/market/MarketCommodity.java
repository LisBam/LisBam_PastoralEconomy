package lisbam.pastoraleconomy.market;

import net.minecraft.item.Item;

/** Immutable economic definition. Its key is independent from the Item/meta identity. */
public final class MarketCommodity {
    private final String key;
    private final Item item;
    private final int metadata;
    private final long basePrice;
    private final CommodityCategory category;
    private final boolean historyTracked;
    private final String variantIdentity;

    MarketCommodity(
            String key,
            Item item,
            int metadata,
            long basePrice,
            CommodityCategory category,
            boolean historyTracked,
            String variantIdentity
    ) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Market commodity key must not be empty.");
        }
        if (item == null || metadata < 0 || basePrice <= 0L || category == null) {
            throw new IllegalArgumentException("Invalid market commodity definition: " + key);
        }
        if (!MarketPriceGenerator.isBasePriceSupported(basePrice, category)) {
            throw new IllegalArgumentException("Base price can overflow the market formula: " + key);
        }
        if (variantIdentity == null || variantIdentity.trim().isEmpty()) {
            throw new IllegalArgumentException("Variant identity must not be empty: " + key);
        }
        this.key = key;
        this.item = item;
        this.metadata = metadata;
        this.basePrice = basePrice;
        this.category = category;
        this.historyTracked = historyTracked;
        this.variantIdentity = variantIdentity;
    }

    public String getKey() {
        return key;
    }

    public Item getItem() {
        return item;
    }

    public int getMetadata() {
        return metadata;
    }

    public long getBasePrice() {
        return basePrice;
    }

    public CommodityCategory getCategory() {
        return category;
    }

    public boolean isHistoryTracked() {
        return historyTracked;
    }

    /** Distinguishes NBT-backed future variants such as enchanted books. */
    public String getVariantIdentity() {
        return variantIdentity;
    }

    /** The market's wool entry accepts every vanilla colour, not only white wool. */
    public boolean isAnyWoolColor() {
        return "minecraft:wool@any_color".equals(variantIdentity);
    }
}
