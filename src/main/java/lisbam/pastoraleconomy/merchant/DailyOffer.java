package lisbam.pastoraleconomy.merchant;

import net.minecraft.nbt.NBTTagCompound;

/** One persisted merchant daily offer. Stock is measured in vanilla maximum-stack groups. */
public final class DailyOffer {
    private static final String KEY_CATALOG = "catalog";
    private static final String KEY_ENABLED = "enabled";
    // Keep the released NBT key so existing offers retain their stock count.
    private static final String KEY_REMAINING_GROUPS = "remainingBundles";
    private static final String KEY_ENCHANTMENT_LEVEL = "enchantmentLevel";

    private final String catalogKey;
    private final boolean enabled;
    private int remainingGroups;
    private final int enchantmentLevel;

    public DailyOffer(String catalogKey, boolean enabled, int remainingGroups) {
        this(catalogKey, enabled, remainingGroups, 0);
    }

    public DailyOffer(String catalogKey, boolean enabled, int remainingGroups, int enchantmentLevel) {
        if (enabled && (catalogKey == null || catalogKey.isEmpty())) {
            throw new IllegalArgumentException("Enabled offers require a catalog key.");
        }
        if (remainingGroups < TradeCatalogEntry.UNLIMITED_STOCK || enchantmentLevel < 0) {
            throw new IllegalArgumentException("Invalid remaining group count.");
        }
        this.catalogKey = catalogKey == null ? "" : catalogKey;
        this.enabled = enabled;
        this.remainingGroups = remainingGroups;
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

    public int getRemainingGroups() {
        return remainingGroups;
    }

    public boolean isUnlimited() {
        return remainingGroups == TradeCatalogEntry.UNLIMITED_STOCK;
    }

    public int getEnchantmentLevel() {
        return enchantmentLevel;
    }

    public boolean canConsumeGroups(int groups) {
        return groups > 0 && (isUnlimited() || remainingGroups >= groups);
    }

    public boolean consumeGroups(int groups) {
        if (!canConsumeGroups(groups)) {
            return false;
        }
        if (!isUnlimited()) {
            remainingGroups -= groups;
        }
        return true;
    }

    public void restoreGroups(int groups) {
        if (groups > 0 && !isUnlimited()) {
            if (remainingGroups > Integer.MAX_VALUE - groups) {
                remainingGroups = Integer.MAX_VALUE;
            } else {
                remainingGroups += groups;
            }
        }
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(KEY_CATALOG, catalogKey);
        tag.setBoolean(KEY_ENABLED, enabled);
        tag.setInteger(KEY_REMAINING_GROUPS, remainingGroups);
        tag.setInteger(KEY_ENCHANTMENT_LEVEL, enchantmentLevel);
        return tag;
    }

    public static DailyOffer readFromNBT(NBTTagCompound tag) {
        String key = tag.getString(KEY_CATALOG);
        boolean enabled = tag.getBoolean(KEY_ENABLED) && !key.isEmpty();
        int remaining = tag.hasKey(KEY_REMAINING_GROUPS) ? tag.getInteger(KEY_REMAINING_GROUPS) : TradeCatalogEntry.UNLIMITED_STOCK;
        if (remaining < TradeCatalogEntry.UNLIMITED_STOCK) {
            remaining = TradeCatalogEntry.UNLIMITED_STOCK;
        }
        int level = Math.max(0, tag.getInteger(KEY_ENCHANTMENT_LEVEL));
        return new DailyOffer(key, enabled, remaining, level);
    }
}
