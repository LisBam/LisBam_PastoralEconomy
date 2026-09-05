package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.market.MarketCatalog;
import lisbam.pastoraleconomy.market.MarketCommodity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.Random;

/** One Treasure-pool enchanted-book candidate with weighted server-side levels. */
public final class EnchantmentTradeDefinition {
    private static final double TREASURE_VOLATILITY = 0.08D;

    private final String candidateId;
    private final Enchantment enchantment;
    private final int[] weights;
    private final long[] prices;
    private final MarketCommodity[] commodities;

    public EnchantmentTradeDefinition(String candidateId, Enchantment enchantment, int[] weights,
                                      long[] prices, String[] marketKeys) {
        if (candidateId == null || candidateId.trim().isEmpty() || enchantment == null
                || enchantment.getRegistryName() == null || weights == null || prices == null || marketKeys == null
                || weights.length == 0 || weights.length != prices.length || weights.length != marketKeys.length
                || weights.length > enchantment.getMaxLevel()) {
            throw new IllegalArgumentException("Invalid enchanted-book trade definition: " + candidateId);
        }
        int totalWeight = 0;
        this.commodities = new MarketCommodity[marketKeys.length];
        for (int index = 0; index < weights.length; index++) {
            if (weights[index] <= 0 || prices[index] <= 0L || marketKeys[index] == null) {
                throw new IllegalArgumentException("Invalid enchantment level data: " + candidateId);
            }
            totalWeight += weights[index];
            MarketCommodity commodity = MarketCatalog.get(marketKeys[index]);
            if (commodity == null || Math.abs(commodity.getCategory().getVolatility() - TREASURE_VOLATILITY) > 0.0000001D) {
                throw new IllegalArgumentException("Missing or mismatched enchantment market key: " + marketKeys[index]);
            }
            this.commodities[index] = commodity;
        }
        if (totalWeight != 100) {
            throw new IllegalArgumentException("Enchantment level weights must total 100: " + candidateId);
        }
        this.candidateId = candidateId;
        this.enchantment = enchantment;
        this.weights = Arrays.copyOf(weights, weights.length);
        this.prices = Arrays.copyOf(prices, prices.length);
    }

    public String getCandidateId() {
        return candidateId;
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }

    public ResourceLocation getEnchantmentRegistryName() {
        return enchantment.getRegistryName();
    }

    public int getMaxLevel() {
        return weights.length;
    }

    public int[] getWeights() {
        return Arrays.copyOf(weights, weights.length);
    }

    public int resolveLevel(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("Random source is required.");
        }
        int roll = random.nextInt(100);
        int cumulative = 0;
        for (int index = 0; index < weights.length; index++) {
            cumulative += weights[index];
            if (roll < cumulative) {
                return index + 1;
            }
        }
        return weights.length;
    }

    public long getPrice(int level) {
        return prices[validateLevel(level) - 1];
    }

    public String getMarketKey(int level) {
        return commodities[validateLevel(level) - 1].getKey();
    }

    public MarketCommodity getCommodity(int level) {
        return commodities[validateLevel(level) - 1];
    }

    public ItemStack createBookStack(int count, int level) {
        if (count <= 0) {
            throw new IllegalArgumentException("Book count must be positive.");
        }
        ItemStack stack = ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(enchantment, validateLevel(level)));
        stack.setCount(count);
        return stack;
    }

    private int validateLevel(int level) {
        if (level < 1 || level > weights.length) {
            throw new IllegalArgumentException("Invalid resolved enchantment level: " + level);
        }
        return level;
    }
}
