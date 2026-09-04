package lisbam.pastoraleconomy.merchant;

import net.minecraft.nbt.NBTTagCompound;

/** One persisted merchant daily offer. Finite stock is measured in individual items. */
public final class DailyOffer {
    private static final String KEY_CATALOG = "catalog";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_REMAINING_ITEMS = "remainingItems";
    private static final String KEY_ENCHANTMENT_LEVEL = "enchantmentLevel";

    private final String catalogKey;
    private final boolean enabled;
    private int remainingItems;
    private final int enchantmentLevel;

    public DailyOffer(String catalogKey, boolean enabled, int remainingItems) {
        this(catalogKey, enabled, remainingItems, 0);
    }

    public DailyOffer(String catalogKey, boolean enabled, int remainingItems, int enchantmentLevel) {
        if (enabled && (catalogKey == null || catalogKey.isEmpty())) {
            throw new IllegalArgumentException("Enabled offers require a catalog key.");
        }
        if (remainingItems < TradeCatalogEntry.UNLIMITED_STOCK || enchantmentLevel < 0) {
            throw new IllegalArgumentException("Invalid remaining item count.");
        }
        this.catalogKey = catalogKey == null ? "" : catalogKey;
        this.enabled = enabled;
        this.remainingItems = remainingItems;
        this.enchantmentLevel = enchantmentLevel;
    }

    public static DailyOffer disabledPlaceholder() {
        return new DailyOffer("", false, TradeCatalogEntry.UNLIMITED_STOCK);
    }

    public String getCatalogKey() {
        return catalogKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getRemainingItems() {
        return remainingItems;
    }

    public boolean isUnlimited() {
        return remainingItems == TradeCatalogEntry.UNLIMITED_STOCK;
    }

    public int getEnchantmentLevel() {
        return enchantmentLevel;
    }

    public boolean canConsumeItems(int items) {
        return items > 0 && (isUnlimited() || remainingItems >= items);
    }

    public boolean consumeItems(int items) {
        if (!canConsumeItems(items)) {
            return false;
        }
        if (!isUnlimited()) {
            remainingItems -= items;
        }
        return true;
    }

    public void restoreItems(int items) {
        if (items > 0 && !isUnlimited()) {
            if (remainingItems > Integer.MAX_VALUE - items) {
                remainingItems = Integer.MAX_VALUE;
            } else {
                remainingItems += items;
            }
        }
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(KEY_CATALOG, catalogKey);
        tag.setBoolean(KEY_ENABLED, enabled);
        tag.setInteger(KEY_REMAINING_ITEMS, remainingItems);
        tag.setInteger(KEY_ENCHANTMENT_LEVEL, enchantmentLevel);
        return tag;
    }

    public static DailyOffer readFromNBT(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(KEY_REMAINING_ITEMS)) {
            throw new IllegalArgumentException("Merchant offer is missing the current item-count field.");
        }
        String key = tag.getString(KEY_CATALOG);
        boolean enabled = tag.getBoolean(KEY_ENABLED) && !key.isEmpty();
        int remaining = tag.getInteger(KEY_REMAINING_ITEMS);
        if (remaining < TradeCatalogEntry.UNLIMITED_STOCK) {
            throw new IllegalArgumentException("Merchant offer has an invalid item count.");
        }
        int level = Math.max(0, tag.getInteger(KEY_ENCHANTMENT_LEVEL));
        return new DailyOffer(key, enabled, remaining, level);
    }
}
