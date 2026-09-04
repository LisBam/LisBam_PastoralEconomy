package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.market.MarketCommodity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

/**
 * One data-driven merchant good. Its stable catalog key is deliberately
 * separate from the Minecraft Item/meta identity and the MarketService key.
 */
public final class TradeCatalogEntry {
    public static final int UNLIMITED_STOCK = -1;

    private final String catalogKey;
    private final String marketKey;
    private final TradePool pool;
    private final int groupSize;
    private final int initialRemainingGroups;
    private final boolean anyWoolColor;
    private final MarketCommodity marketCommodity;
    private final EnchantmentTradeDefinition enchantmentDefinition;

    TradeCatalogEntry(String catalogKey, String marketKey, TradePool pool, int initialRemainingGroups,
                      boolean anyWoolColor, MarketCommodity marketCommodity) {
        this(catalogKey, marketKey, pool, initialRemainingGroups, anyWoolColor, marketCommodity, null);
    }

    TradeCatalogEntry(String catalogKey, String marketKey, TradePool pool, int initialRemainingGroups,
                      boolean anyWoolColor, MarketCommodity marketCommodity,
                      EnchantmentTradeDefinition enchantmentDefinition) {
        if (catalogKey == null || catalogKey.isEmpty() || marketKey == null || marketKey.isEmpty()
                || pool == null || marketCommodity == null
                || (initialRemainingGroups != UNLIMITED_STOCK && initialRemainingGroups <= 0)) {
            throw new IllegalArgumentException("Invalid merchant catalog entry.");
        }
        int vanillaMaxStackSize = marketCommodity.getItem().getItemStackLimit();
        if (vanillaMaxStackSize <= 0) {
            throw new IllegalArgumentException("Merchant item must have a positive vanilla stack limit.");
        }
        this.catalogKey = catalogKey;
        this.marketKey = marketKey;
        this.pool = pool;
        // A group is exactly one vanilla maximum stack, never a catalog-defined amount.
        this.groupSize = vanillaMaxStackSize;
        this.initialRemainingGroups = initialRemainingGroups;
        this.anyWoolColor = anyWoolColor;
        this.marketCommodity = marketCommodity;
        this.enchantmentDefinition = enchantmentDefinition;
    }

    public String getCatalogKey() {
        return catalogKey;
    }

    public String getMarketKey() {
        return marketKey;
    }

    public TradePool getPool() {
        return pool;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public int getInitialRemainingGroups() {
        return initialRemainingGroups;
    }

    public boolean isUnlimitedStock() {
        return initialRemainingGroups == UNLIMITED_STOCK;
    }

    public boolean isAnyWoolColor() {
        return anyWoolColor;
    }

    public long getBasePrice() {
        return marketCommodity.getBasePrice();
    }

    public double getVolatility() {
        return marketCommodity.getCategory().getVolatility();
    }

    public boolean isEnchantment() {
        return enchantmentDefinition != null;
    }

    public EnchantmentTradeDefinition getEnchantmentDefinition() {
        return enchantmentDefinition;
    }

    public int resolveEnchantmentLevel(java.util.Random random) {
        return enchantmentDefinition == null ? 0 : enchantmentDefinition.resolveLevel(random);
    }

    public String getMarketKeyForLevel(int level) {
        return enchantmentDefinition == null ? (level == 0 ? marketKey : null)
                : enchantmentDefinition.getMarketKey(level);
    }

    public long getBasePriceForLevel(int level) {
        return enchantmentDefinition == null ? (level == 0 ? getBasePrice() : -1L)
                : enchantmentDefinition.getPrice(level);
    }

    public ItemStack createStack(int count) {
        return createStack(count, isEnchantment() ? 1 : 0);
    }

    public ItemStack createStack(int count, int enchantmentLevel) {
        if (count <= 0) {
            throw new IllegalArgumentException("Trade output count must be positive.");
        }
        if (enchantmentDefinition != null) {
            return enchantmentDefinition.createBookStack(count, enchantmentLevel);
        }
        return new ItemStack(marketCommodity.getItem(), count, marketCommodity.getMetadata());
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != marketCommodity.getItem()) {
            return false;
        }
        if (enchantmentDefinition != null) {
            return false;
        }
        if (!anyWoolColor) {
            return stack.getMetadata() == marketCommodity.getMetadata();
        }
        return marketCommodity.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.WOOL)
                && stack.getMetadata() >= 0 && stack.getMetadata() < 16;
    }
}
