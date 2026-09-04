package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.market.MarketService;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Generates one persisted 6 + 10 offer layout per merchant and market day. */
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
            if (hasAdvancedOffers(existing)) {
                return false;
            }
            // v4 -> v5 migration: preserve the existing sell/common/uncommon offers
            // and only fill the three placeholders introduced by this batch.
            List<DailyOffer> sell = new ArrayList<DailyOffer>(existing.getSellOffers());
            List<DailyOffer> buy = new ArrayList<DailyOffer>(existing.getBuyOffers());
            while (buy.size() > 7) {
                buy.remove(buy.size() - 1);
            }
            appendPoolOffers(merchant, TradePool.BUY_RARE, 2, buy, worldDay);
            appendPoolOffers(merchant, TradePool.BUY_TREASURE, 1, buy, worldDay);
            merchant.setDailyOfferState(new DailyOfferState(worldDay, sell, buy));
            return true;
        }

        List<DailyOffer> sell = new ArrayList<DailyOffer>(DailyOfferState.SELL_OFFER_COUNT);
        appendPoolOffers(merchant, TradePool.SELL_CORE, 4, sell, worldDay);
        appendPoolOffers(merchant, TradePool.SELL_SECONDARY, 2, sell, worldDay);

        List<DailyOffer> buy = new ArrayList<DailyOffer>(DailyOfferState.BUY_OFFER_COUNT);
        appendPoolOffers(merchant, TradePool.BUY_COMMON, 4, buy, worldDay);
        appendPoolOffers(merchant, TradePool.BUY_UNCOMMON, 3, buy, worldDay);
        appendPoolOffers(merchant, TradePool.BUY_RARE, 2, buy, worldDay);
        appendPoolOffers(merchant, TradePool.BUY_TREASURE, 1, buy, worldDay);
        merchant.setDailyOfferState(new DailyOfferState(worldDay, sell, buy));
        return true;
    }

    private static boolean hasAdvancedOffers(DailyOfferState state) {
        return isOfferFromPool(state.getBuyOffer(7), TradePool.BUY_RARE)
                && isOfferFromPool(state.getBuyOffer(8), TradePool.BUY_RARE)
                && isOfferFromPool(state.getBuyOffer(9), TradePool.BUY_TREASURE);
    }

    private static boolean isOfferFromPool(DailyOffer offer, TradePool pool) {
        if (offer == null || !offer.isEnabled()) {
            return false;
        }
        TradeCatalogEntry entry = TradeCatalog.get(offer.getCatalogKey());
        if (entry == null || entry.getPool() != pool) {
            return false;
        }
        if (entry.isEnchantment()) {
            return offer.getEnchantmentLevel() >= 1
                    && offer.getEnchantmentLevel() <= entry.getEnchantmentDefinition().getMaxLevel();
        }
        return offer.getEnchantmentLevel() == 0;
    }

    private static void appendPoolOffers(MerchantRecord merchant, TradePool pool, int count, List<DailyOffer> output,
                                         long worldDay) {
        for (int index = 0; index < count; index++) {
            TradeCatalogEntry entry = takeNext(merchant, pool);
            int level = 0;
            if (entry.isEnchantment()) {
                long seed = merchant.getMerchantId().getMostSignificantBits()
                        ^ Long.rotateLeft(merchant.getMerchantId().getLeastSignificantBits(), 23)
                        ^ Long.rotateLeft(worldDay, 11)
                        ^ ((long) entry.getCatalogKey().hashCode() * 0x9E3779B97F4A7C15L);
                level = entry.resolveEnchantmentLevel(new Random(seed));
            }
            output.add(new DailyOffer(entry.getCatalogKey(), true, entry.getInitialRemainingBundles(), level));
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
