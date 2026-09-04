package lisbam.pastoraleconomy.merchant;

import net.minecraft.nbt.NBTTagCompound;

/** One persisted merchant daily offer. Stock is measured only in bundles. */
public final class DailyOffer {
    private static final String KEY_CATALOG = "catalog";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_REMAINING = "remainingBundles";
    private static final String KEY_ENCHANTMENT_LEVEL = "enchantmentLevel";

    private final String catalogKey;
    private final boolean enabled;
    private int remainingBundles;
    private final int enchantmentLevel;

    public DailyOffer(String catalogKey, boolean enabled, int remainingBundles) {
        this(catalogKey, enabled, remainingBundles, 0);
    }

    public DailyOffer(String catalogKey, boolean enabled, int remainingBundles, int enchantmentLevel) {
        if (enabled && (catalogKey == null || catalogKey.isEmpty())) {
            throw new IllegalArgumentException("Enabled offers require a catalog key.");
        }
        if (remainingBundles < TradeCatalogEntry.UNLIMITED_STOCK || enchantmentLevel < 0) {
            throw new IllegalArgumentException("Invalid remaining bundle count.");
        }
        this.catalogKey = catalogKey == null ? "" : catalogKey;
        this.enabled = enabled;
        this.remainingBundles = remainingBundles;
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

    public int getRemainingBundles() {
        return remainingBundles;
    }

    public boolean isUnlimited() {
        return remainingBundles == TradeCatalogEntry.UNLIMITED_STOCK;
    }

    public int getEnchantmentLevel() {
        return enchantmentLevel;
    }

    public boolean canConsume(int bundles) {
        return bundles > 0 && (isUnlimited() || remainingBundles >= bundles);
    }

    public boolean consume(int bundles) {
        if (!canConsume(bundles)) {
            return false;
        }
        if (!isUnlimited()) {
            remainingBundles -= bundles;
        }
        return true;
    }

    public void restore(int bundles) {
        if (bundles > 0 && !isUnlimited()) {
            if (remainingBundles > Integer.MAX_VALUE - bundles) {
                remainingBundles = Integer.MAX_VALUE;
            } else {
                remainingBundles += bundles;
            }
        }
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(KEY_CATALOG, catalogKey);
        tag.setBoolean(KEY_ENABLED, enabled);
        tag.setInteger(KEY_REMAINING, remainingBundles);
        tag.setInteger(KEY_ENCHANTMENT_LEVEL, enchantmentLevel);
        return tag;
    }

    public static DailyOffer readFromNBT(NBTTagCompound tag) {
        String key = tag.getString(KEY_CATALOG);
        boolean enabled = tag.getBoolean(KEY_ENABLED) && !key.isEmpty();
        int remaining = tag.hasKey(KEY_REMAINING) ? tag.getInteger(KEY_REMAINING) : TradeCatalogEntry.UNLIMITED_STOCK;
        if (remaining < TradeCatalogEntry.UNLIMITED_STOCK) {
            remaining = TradeCatalogEntry.UNLIMITED_STOCK;
        }
        int level = Math.max(0, tag.getInteger(KEY_ENCHANTMENT_LEVEL));
        return new DailyOffer(key, enabled, remaining, level);
    }
}
