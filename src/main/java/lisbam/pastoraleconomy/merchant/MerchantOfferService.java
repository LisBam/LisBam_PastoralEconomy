package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.market.MarketService;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Generates one persisted 6 + 8 offer layout per merchant and market day. */
public final class MerchantOfferService {

    private MerchantOfferService() {
    }

    /** @return true only if a new world-day offer state was created. */
    public static boolean ensureOffers(World world, MerchantRecord merchant) {
        if (world == null || world.isRemote || merchant == null) {
            throw new IllegalArgumentException("Merchant offers require a logical-server world and record.");
        }
        long worldDay = MarketService.getCurrentMarketDay(world);
        DailyOfferState existing = merchant.getDailyOfferState();
        if (existing != null && existing.getWorldDay() == worldDay) {
            if (hasSortedBuyOffers(existing) && hasUniqueBuyProducts(existing)) {
                return false;
            }
            // Invalid or incomplete same-day states are regenerated under the
            // current item-count, quality-order, and uniqueness rules.
            merchant.setDailyOfferState(createOfferState(merchant, worldDay));
            return true;
        }

        merchant.setDailyOfferState(createOfferState(merchant, worldDay));
        return true;
    }

    private static DailyOfferState createOfferState(MerchantRecord merchant, long worldDay) {
        List<DailyOffer> sell = new ArrayList<DailyOffer>(DailyOfferState.SELL_OFFER_COUNT);
        appendPoolOffers(merchant, TradePool.SELL_CORE, 4, sell, worldDay, null);
        appendPoolOffers(merchant, TradePool.SELL_SECONDARY, 2, sell, worldDay, null);

        return new DailyOfferState(worldDay, sell, createPurchaseOffers(merchant, worldDay));
    }

    private static List<DailyOffer> createPurchaseOffers(MerchantRecord merchant, long worldDay) {
        List<DailyOffer> buy = new ArrayList<DailyOffer>(DailyOfferState.BUY_OFFER_COUNT);
        Set<String> selectedProducts = new HashSet<String>();
        Random qualityRandom = new Random(createPurchaseQualitySeed(merchant, worldDay));
        for (int slot = 0; slot < DailyOfferState.BUY_OFFER_COUNT; slot++) {
            appendPoolOffers(merchant, selectPurchasePool(qualityRandom), 1, buy, worldDay, selectedProducts);
        }
        sortPurchaseOffers(buy);
        return buy;
    }

    private static long createPurchaseQualitySeed(MerchantRecord merchant, long worldDay) {
        return merchant.getMerchantId().getMostSignificantBits()
                ^ Long.rotateLeft(merchant.getMerchantId().getLeastSignificantBits(), 31)
                ^ Long.rotateLeft(worldDay, 7)
                ^ 0xA0761D6478BD642FL;
    }

    /** Each purchase slot independently selects a quality by its frozen daily weight. */
    static TradePool selectPurchasePool(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("Purchase quality random cannot be null.");
        }
        int roll = random.nextInt(100);
        if (roll < 40) {
            return TradePool.BUY_COMMON;
        }
        if (roll < 70) {
            return TradePool.BUY_UNCOMMON;
        }
        if (roll < 90) {
            return TradePool.BUY_RARE;
        }
        return TradePool.BUY_TREASURE;
    }

    /** Sorts generated purchase offers into the UI's low-to-high quality grouping. */
    static void sortPurchaseOffers(List<DailyOffer> offers) {
        if (offers == null) {
            throw new IllegalArgumentException("Purchase offers cannot be null.");
        }
        Collections.sort(offers, new Comparator<DailyOffer>() {
            @Override
            public int compare(DailyOffer left, DailyOffer right) {
                return getPurchaseQualityRank(left) - getPurchaseQualityRank(right);
            }
        });
    }

    static int getPurchaseQualityRank(DailyOffer offer) {
        if (offer == null || !offer.isEnabled()) {
            return -1;
        }
        TradeCatalogEntry entry = TradeCatalog.get(offer.getCatalogKey());
        if (entry == null) {
            return -1;
        }
        switch (entry.getPool()) {
            case BUY_COMMON:
                return 0;
            case BUY_UNCOMMON:
                return 1;
            case BUY_RARE:
                return 2;
            case BUY_TREASURE:
                return 3;
            default:
                return -1;
        }
    }

    private static boolean hasSortedBuyOffers(DailyOfferState state) {
        if (state == null) {
            return false;
        }
        int previousRank = -1;
        for (DailyOffer offer : state.getBuyOffers()) {
            int rank = getPurchaseQualityRank(offer);
            if (rank < 0 || rank < previousRank) {
                return false;
            }
            previousRank = rank;
        }
        return true;
    }

    private static boolean hasUniqueBuyProducts(DailyOfferState state) {
        return state != null && productIdentities(state.getBuyOffers()).size() == DailyOfferState.BUY_OFFER_COUNT;
    }

    private static Set<String> productIdentities(List<DailyOffer> offers) {
        Set<String> result = new HashSet<String>();
        if (offers == null) {
            return result;
        }
        for (DailyOffer offer : offers) {
            if (offer == null || !offer.isEnabled()) {
                continue;
            }
            TradeCatalogEntry entry = TradeCatalog.get(offer.getCatalogKey());
            if (entry != null) {
                result.add(entry.getProductIdentity(offer.getEnchantmentLevel()));
            }
        }
        return result;
    }

    private static void appendPoolOffers(MerchantRecord merchant, TradePool pool, int count, List<DailyOffer> output,
                                         long worldDay, Set<String> selectedProducts) {
        int attempts = 0;
        int maxAttempts = Math.max(32, TradeCatalog.getPool(pool).size() * 4);
        while (count > 0 && attempts++ < maxAttempts) {
            TradeCatalogEntry entry = takeNext(merchant, pool);
            int level = 0;
            if (entry.isEnchantment()) {
                long seed = merchant.getMerchantId().getMostSignificantBits()
                        ^ Long.rotateLeft(merchant.getMerchantId().getLeastSignificantBits(), 23)
                        ^ Long.rotateLeft(worldDay, 11)
                        ^ ((long) entry.getCatalogKey().hashCode() * 0x9E3779B97F4A7C15L);
                level = entry.resolveEnchantmentLevel(new Random(seed));
            }
            String identity = entry.getProductIdentity(level);
            if (selectedProducts != null && !selectedProducts.add(identity)) {
                continue;
            }
            output.add(new DailyOffer(entry.getCatalogKey(), true, entry.getInitialRemainingItems(), level));
            count--;
        }
        if (count > 0) {
            throw new IllegalStateException("Merchant offer pool cannot provide enough unique products: " + pool);
        }
    }

    private static TradeCatalogEntry takeNext(MerchantRecord merchant, TradePool pool) {
        List<TradeCatalogEntry> source = TradeCatalog.getPool(pool);
        if (source.isEmpty()) {
            throw new IllegalStateException("Merchant offer pool is empty: " + pool);
        }
        OfferRotationState rotation = merchant.getRotation(pool);
        if (!isUsableBag(rotation, source)) {
            refillBag(merchant, pool, rotation, source);
        }
        String key = rotation.takeNext();
        if (key == null) {
            refillBag(merchant, pool, rotation, source);
            key = rotation.takeNext();
        }
        TradeCatalogEntry entry = TradeCatalog.get(key);
        if (entry == null || entry.getPool() != pool) {
            throw new IllegalStateException("Merchant offer rotation contains an invalid catalog key.");
        }
        return entry;
    }

    private static boolean isUsableBag(OfferRotationState state, List<TradeCatalogEntry> source) {
        if (state.getCursor() >= state.getBag().size() || state.getBag().size() != source.size()) {
            return false;
        }
        Set<String> expected = new HashSet<String>();
        for (TradeCatalogEntry entry : source) {
            expected.add(entry.getCatalogKey());
        }
        return expected.size() == state.getBag().size() && expected.containsAll(state.getBag());
    }

    private static void refillBag(MerchantRecord merchant, TradePool pool, OfferRotationState state,
                                  List<TradeCatalogEntry> source) {
        List<String> keys = new ArrayList<String>(source.size());
        for (TradeCatalogEntry entry : source) {
            keys.add(entry.getCatalogKey());
        }
        long seed = merchant.getMerchantId().getMostSignificantBits()
                ^ Long.rotateLeft(merchant.getMerchantId().getLeastSignificantBits(), 17)
                ^ ((long) pool.ordinal() * 0x9E3779B97F4A7C15L)
                ^ ((long) (state.getCycle() + 1) * 0xD1B54A32D192ED03L);
        Collections.shuffle(keys, new Random(seed));
        state.replaceBag(keys);
    }
}
