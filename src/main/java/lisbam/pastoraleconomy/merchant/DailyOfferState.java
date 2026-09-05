package lisbam.pastoraleconomy.merchant;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Persisted offer and stock state for exactly one logical merchant and one world day. */
public final class DailyOfferState {
    public static final int SELL_OFFER_COUNT = 6;
    public static final int BUY_OFFER_COUNT = 8;

    private static final String KEY_DAY = "worldDay";
    private static final String KEY_SELL = "sellOffers";
    private static final String KEY_BUY = "buyOffers";

    private final long worldDay;
    private final List<DailyOffer> sellOffers;
    private final List<DailyOffer> buyOffers;

    public DailyOfferState(long worldDay, List<DailyOffer> sellOffers, List<DailyOffer> buyOffers) {
        if (worldDay < 0L || sellOffers == null || buyOffers == null
                || sellOffers.size() != SELL_OFFER_COUNT || buyOffers.size() != BUY_OFFER_COUNT) {
            throw new IllegalArgumentException("Invalid daily merchant offer state.");
        }
        Set<String> products = new HashSet<String>();
        for (DailyOffer offer : buyOffers) {
            if (offer == null || !offer.isEnabled()) {
                continue;
            }
            TradeCatalogEntry entry = TradeCatalog.get(offer.getCatalogKey());
            if (!isValidOffer(entry, offer, false)
                    || !products.add(entry.getProductIdentity(offer.getEnchantmentLevel()))) {
                throw new IllegalArgumentException("Daily merchant purchase offers must be unique.");
            }
        }
        for (DailyOffer offer : sellOffers) {
            if (offer != null && offer.isEnabled()) {
                TradeCatalogEntry entry = TradeCatalog.get(offer.getCatalogKey());
                if (!isValidOffer(entry, offer, true)) {
                    throw new IllegalArgumentException("Invalid daily merchant sale offer.");
                }
            }
        }
        this.worldDay = worldDay;
        this.sellOffers = new ArrayList<DailyOffer>(sellOffers);
        this.buyOffers = new ArrayList<DailyOffer>(buyOffers);
    }

    public long getWorldDay() {
        return worldDay;
    }

    public List<DailyOffer> getSellOffers() {
        return Collections.unmodifiableList(sellOffers);
    }

    public List<DailyOffer> getBuyOffers() {
        return Collections.unmodifiableList(buyOffers);
    }

    public DailyOffer getSellOffer(int slot) {
        return slot < 0 || slot >= sellOffers.size() ? null : sellOffers.get(slot);
    }

    public DailyOffer getBuyOffer(int slot) {
        return slot < 0 || slot >= buyOffers.size() ? null : buyOffers.get(slot);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong(KEY_DAY, worldDay);
        tag.setTag(KEY_SELL, writeOffers(sellOffers));
        tag.setTag(KEY_BUY, writeOffers(buyOffers));
        return tag;
    }

    public static DailyOfferState readFromNBT(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(KEY_DAY)) {
            return null;
        }
        try {
            List<DailyOffer> sell = readOffers(tag.getTagList(KEY_SELL, 10), SELL_OFFER_COUNT);
            List<DailyOffer> buy = readOffers(tag.getTagList(KEY_BUY, 10), BUY_OFFER_COUNT);
            if (sell == null || buy == null) {
                return null;
            }
            return new DailyOfferState(tag.getLong(KEY_DAY), sell, buy);
        } catch (RuntimeException ignored) {
            // Invalid or pre-item-count states are regenerated on the logical server.
            return null;
        }
    }

    private static boolean isValidOffer(TradeCatalogEntry entry, DailyOffer offer, boolean selling) {
        if (entry == null || (selling
                ? (entry.getPool() != TradePool.SELL_CORE && entry.getPool() != TradePool.SELL_SECONDARY)
                : (entry.getPool() != TradePool.BUY_COMMON && entry.getPool() != TradePool.BUY_UNCOMMON
                && entry.getPool() != TradePool.BUY_RARE && entry.getPool() != TradePool.BUY_TREASURE))) {
            return false;
        }
        if (!offer.isUnlimited() && (offer.getRemainingItems() < 0
                || offer.getRemainingItems() > entry.getInitialRemainingItems())) {
            return false;
        }
        if (entry.isEnchantment()) {
            return !selling && offer.getEnchantmentLevel() >= 1
                    && offer.getEnchantmentLevel() <= entry.getEnchantmentDefinition().getMaxLevel();
        }
        return offer.getEnchantmentLevel() == 0;
    }

    private static NBTTagList writeOffers(List<DailyOffer> offers) {
        NBTTagList list = new NBTTagList();
        for (DailyOffer offer : offers) {
            list.appendTag(offer.writeToNBT());
        }
        return list;
    }

    private static List<DailyOffer> readOffers(NBTTagList list, int expectedSize) {
        if (list.tagCount() != expectedSize) {
            return null;
        }
        List<DailyOffer> result = new ArrayList<DailyOffer>(expectedSize);
        for (int index = 0; index < expectedSize; index++) {
            result.add(DailyOffer.readFromNBT(list.getCompoundTagAt(index)));
        }
        return result;
    }
}
