package lisbam.pastoraleconomy.merchant;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.market.MarketCatalog;
import lisbam.pastoraleconomy.network.message.SyncMerchantTradeMessage;
import java.util.List;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Deterministic regression checks for the item-priced merchant catalog. */
public final class MerchantCatalogSelfTest {
    private MerchantCatalogSelfTest() {
    }

    public static void main(String[] args) {
        Bootstrap.register();
        verifyMerchantSnapshotRoundTrip();
        check(VillageService.getTargetMerchantCount(2) == 3, "2 villagers");
        check(VillageService.getTargetMerchantCount(15) == 3, "15 villagers");
        check(VillageService.getTargetMerchantCount(16) == 4, "16 villagers");
        check(VillageService.getTargetMerchantCount(20) == 4, "20 villagers");
        check(VillageService.getTargetMerchantCount(21) == 5, "21 villagers");
        check(VillageService.getTargetMerchantCount(35) == 7, "35 villagers");
        check(VillageService.getTargetMerchantCount(36) == 8, "36 villagers");
        check(VillageService.getTargetMerchantCount(100) == 20, "100 villagers");
        check(VillageService.getTargetMerchantCount(1000) == 200, "1000 villagers");
        check(VillageService.MAINTENANCE_INTERVAL_TICKS == 200, "ten-second village maintenance");
        check(VillageService.MERCHANT_ACTIVITY_RADIUS == 32, "merchant activity radius");
        check(VillageService.MERCHANT_TELEPORT_DISTANCE == 64, "merchant teleport distance");

        check(TradeCatalog.getPool(TradePool.SELL_CORE).size() == 9, "core sell pool");
        check(TradeCatalog.getPool(TradePool.SELL_SECONDARY).size() == 20, "secondary sell pool");
        check(TradeCatalog.getPool(TradePool.BUY_COMMON).size() == 56, "common buy pool");
        check(TradeCatalog.getPool(TradePool.BUY_UNCOMMON).size() == 42, "uncommon buy pool");
        check(TradeCatalog.getPool(TradePool.BUY_RARE).size() == 28, "rare buy pool");
        check(TradeCatalog.getPool(TradePool.BUY_TREASURE).size() == 37, "treasure buy pool");
        check(DailyOfferState.BUY_OFFER_COUNT == 8, "eight daily purchase slots");
        check(MerchantOfferService.selectPurchasePool(new FixedRandom(0)) == TradePool.BUY_COMMON,
                "common purchase quality lower boundary");
        check(MerchantOfferService.selectPurchasePool(new FixedRandom(39)) == TradePool.BUY_COMMON,
                "common purchase quality upper boundary");
        check(MerchantOfferService.selectPurchasePool(new FixedRandom(40)) == TradePool.BUY_UNCOMMON,
                "uncommon purchase quality lower boundary");
        check(MerchantOfferService.selectPurchasePool(new FixedRandom(69)) == TradePool.BUY_UNCOMMON,
                "uncommon purchase quality upper boundary");
        check(MerchantOfferService.selectPurchasePool(new FixedRandom(70)) == TradePool.BUY_RARE,
                "rare purchase quality lower boundary");
        check(MerchantOfferService.selectPurchasePool(new FixedRandom(89)) == TradePool.BUY_RARE,
                "rare purchase quality upper boundary");
        check(MerchantOfferService.selectPurchasePool(new FixedRandom(90)) == TradePool.BUY_TREASURE,
                "treasure purchase quality lower boundary");
        check(MerchantOfferService.selectPurchasePool(new FixedRandom(99)) == TradePool.BUY_TREASURE,
                "treasure purchase quality upper boundary");
        check(find(TradePool.SELL_SECONDARY, "beef_sell").getBasePrice() == 120L, "beef sell price");
        check(find(TradePool.SELL_SECONDARY, "porkchop_sell").getBasePrice() == 120L, "porkchop sell price");
        check(find(TradePool.SELL_SECONDARY, "chicken_sell").getBasePrice() == 100L, "chicken sell price");
        check(find(TradePool.SELL_SECONDARY, "mutton_sell").getBasePrice() == 90L, "mutton sell price");
        check(find(TradePool.SELL_SECONDARY, "rabbit_sell").getBasePrice() == 140L, "rabbit sell price");
        check(find(TradePool.SELL_SECONDARY, "fish_sell").getBasePrice() == 80L, "cod sell price");
        check(find(TradePool.SELL_SECONDARY, "salmon_sell").getBasePrice() == 100L, "salmon sell price");

        String[][] rareStocks = {
                {"diamond", "256"}, {"emerald", "256"}, {"slime_ball", "32"}, {"blaze_rod", "256"}, {"ghast_tear", "256"},
                {"ender_pearl", "64"}, {"wither_skeleton_skull", "64"}, {"shulker_shell", "128"},
                {"dragon_breath", "256"}, {"sponge", "128"}, {"chainmail_helmet", "2"},
                {"chainmail_chestplate", "2"}, {"chainmail_leggings", "2"}, {"chainmail_boots", "2"},
                {"iron_horse_armor", "2"}, {"golden_horse_armor", "2"}, {"saddle", "2"},
                {"coal_ore", "128"}, {"iron_ore", "128"}, {"gold_ore", "128"}, {"redstone_ore", "128"},
                {"lapis_ore", "128"}, {"quartz_ore", "128"}, {"experience_bottle", "128"}, {"name_tag", "128"},
                {"skeleton_skull", "128"}, {"zombie_head", "128"}, {"creeper_head", "128"}
        };
        for (String[] row : rareStocks) {
            check(find(TradePool.BUY_RARE, row[0]).getInitialRemainingItems() == Integer.parseInt(row[1]),
                    "rare stock " + row[0]);
        }
        check(find(TradePool.BUY_RARE, "wither_skeleton_skull").createStack(1, 0).getMetadata() == 1,
                "wither skull metadata");
        check(find(TradePool.BUY_RARE, "sponge").createStack(1, 0).getMetadata() == 0,
                "dry sponge metadata");
        check(find(TradePool.BUY_RARE, "coal_ore").createStack(4, 0).getItem() == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.COAL_ORE),
                "coal ore item mapping");
        check(find(TradePool.BUY_TREASURE, "dragon_head").createStack(1, 0).getMetadata() == 5,
                "dragon head metadata");
        check(find(TradePool.BUY_TREASURE, "enchanted_golden_apple").createStack(1, 0).getMetadata() == 1,
                "enchanted golden apple metadata");

        TradeCatalogEntry gold = find(TradePool.BUY_UNCOMMON, "gold_ingot");
        TradeCatalogEntry lava = find(TradePool.BUY_UNCOMMON, "lava_bucket");
        TradeCatalogEntry magma = find(TradePool.BUY_UNCOMMON, "magma_cream");
        TradeCatalogEntry rabbitFoot = find(TradePool.BUY_UNCOMMON, "rabbit_foot_buy");
        check(gold.getInitialRemainingItems() == 256 && gold.getItemStackLimit() == 64, "gold stock");
        check(lava.getInitialRemainingItems() == 4 && lava.getItemStackLimit() == 1, "lava stock");
        check(magma.getInitialRemainingItems() == 256 && magma.getItemStackLimit() == 64, "magma stock");
        check(rabbitFoot.getInitialRemainingItems() == 256 && rabbitFoot.getItemStackLimit() == 64, "rabbit foot stock");

        Set<String> keys = new HashSet<String>();
        for (TradePool pool : TradePool.values()) {
            for (TradeCatalogEntry entry : TradeCatalog.getPool(pool)) {
                check(keys.add(entry.getCatalogKey()), "duplicate catalog key");
                check(entry.getBasePrice() > 0L, "positive base price");
                check(entry.getItemStackLimit() > 0, "positive stack limit");
                check(entry.getInitialRemainingItems() > 0 || entry.isUnlimitedStock(), "valid stock");
                if (pool == TradePool.BUY_COMMON || pool == TradePool.BUY_UNCOMMON
                        || pool == TradePool.BUY_RARE || pool == TradePool.BUY_TREASURE) {
                    check(MarketCatalog.get(entry.getMarketKey()) != null
                                    && MarketCatalog.get(entry.getMarketKey()).isHistoryTracked(),
                            "all merchant purchase goods need market-book history");
                }
                ItemStack singleItem = entry.createStack(1, entry.isEnchantment() ? 1 : 0);
                check(entry.getItemStackLimit() == singleItem.getMaxStackSize(),
                        "merchant stack limit must equal vanilla maximum stack size");
                if (pool == TradePool.BUY_TREASURE) {
                    check(entry.getInitialRemainingItems() == 1, "treasure stock is one item");
                    check(Math.abs(entry.getVolatility() - 0.08D) < 0.0000001D, "treasure volatility");
                }
                if (pool == TradePool.BUY_RARE) {
                    check(!entry.isUnlimitedStock(), "rare stock is finite");
                    check(Math.abs(entry.getVolatility() - 0.18D) < 0.0000001D
                            || Math.abs(entry.getVolatility() - 0.12D) < 0.0000001D, "rare volatility");
                }
                ItemStack stack = entry.createStack(entry.getItemStackLimit(), entry.isEnchantment() ? 1 : 0);
                check(stack != null && !stack.isEmpty() && stack.getItem() != Items.AIR, "item exists");
                check(stack.getCount() == entry.getItemStackLimit(), "stack output count");
                if (entry.isEnchantment()) {
                    EnchantmentTradeDefinition definition = entry.getEnchantmentDefinition();
                    int[] weights = definition.getWeights();
                    int total = 0;
                    for (int level = 1; level <= definition.getMaxLevel(); level++) {
                        total += weights[level - 1];
                        check(definition.getPrice(level) > 0L, "enchantment level price");
                        check(definition.getMarketKey(level) != null, "enchantment market key");
                        ItemStack book = entry.createStack(1, level);
                        check(book.getItem() == Items.ENCHANTED_BOOK
                                && ItemEnchantedBook.getEnchantments(book).tagCount() > 0, "real enchanted book");
                    }
                    check(total == 100, "enchantment weights");
                }
            }
        }
        int records = 0;
        for (TradeCatalogEntry entry : TradeCatalog.getPool(TradePool.BUY_TREASURE)) {
            if (entry.getCatalogKey().contains("record_")) {
                records++;
            }
        }
        check(records == 12, "exactly twelve music discs");
        TradeCatalogEntry harvest = find(TradePool.BUY_TREASURE, "treasure_enchant_harvest");
        EnchantmentTradeDefinition harvestDefinition = harvest.getEnchantmentDefinition();
        check(harvestDefinition.resolveLevel(new FixedRandom(0)) == 1, "harvest level lower boundary");
        check(harvestDefinition.resolveLevel(new FixedRandom(49)) == 1, "harvest level I boundary");
        check(harvestDefinition.resolveLevel(new FixedRandom(50)) == 2, "harvest level II boundary");
        check(harvestDefinition.resolveLevel(new FixedRandom(84)) == 2, "harvest level II upper boundary");
        check(harvestDefinition.resolveLevel(new FixedRandom(85)) == 3, "harvest level III boundary");
        check(harvestDefinition.resolveLevel(new FixedRandom(99)) == 3, "harvest level upper boundary");
        EnchantmentTradeDefinition attackSpeedDefinition = find(TradePool.BUY_TREASURE,
                "treasure_enchant_attack_speed").getEnchantmentDefinition();
        check(attackSpeedDefinition.resolveLevel(new FixedRandom(0)) == 1, "attack speed level I boundary");
        check(attackSpeedDefinition.resolveLevel(new FixedRandom(39)) == 1, "attack speed level I upper boundary");
        check(attackSpeedDefinition.resolveLevel(new FixedRandom(40)) == 2, "attack speed level II boundary");
        check(attackSpeedDefinition.resolveLevel(new FixedRandom(94)) == 4, "attack speed level IV upper boundary");
        check(attackSpeedDefinition.resolveLevel(new FixedRandom(95)) == 5, "attack speed level V boundary");
        check(find(TradePool.BUY_TREASURE, "treasure_enchant_reforged").getBasePrice() == 120000L,
                "Reforged book price");
        check(find(TradePool.BUY_TREASURE, "treasure_enchant_bluntness_curse").getBasePrice() == 20000L,
                "Bluntness Curse book price");
        ItemStack marketBookHarvest = TradeCatalog.createEnchantedBookStackForMarketKey(
                "lisbam_pastoral_economy:buy/treasure/enchanted_book/harvest_3");
        check(marketBookHarvest != null && marketBookHarvest.getItem() == Items.ENCHANTED_BOOK,
                "market-book enchanted entry builds a native book");
        check(ItemEnchantedBook.getEnchantments(marketBookHarvest).tagCount() == 1
                        && ItemEnchantedBook.getEnchantments(marketBookHarvest).getCompoundTagAt(0).getShort("lvl") == 3,
                "market-book enchanted entry preserves the displayed level");
        check(TradeCatalog.createEnchantedBookStackForMarketKey(
                        "lisbam_pastoral_economy:buy/common/wheat_seeds") == null,
                "ordinary market entries are not converted into enchanted books");
        DailyOffer resolved = new DailyOffer(harvest.getCatalogKey(), true, 1, 3);
        DailyOffer restored = DailyOffer.readFromNBT(resolved.writeToNBT());
        check(restored.getEnchantmentLevel() == 3 && restored.getRemainingItems() == 1,
                "resolved enchantment level persistence");
        check(resolved.canConsumeItems(1) && resolved.consumeItems(1) && !resolved.canConsumeItems(1),
                "finite offers consume individual items");
        check(resolved.getRemainingItems() == 0, "finite offer reaches zero items");
        try {
            DailyOffer.readFromNBT(new NBTTagCompound());
            throw new AssertionError("legacy offer without remainingItems must be rejected");
        } catch (IllegalArgumentException expected) {
            // New saves must carry the item-count field; old group-count saves are not migrated.
        }
        List<DailyOffer> duplicateBuys = new java.util.ArrayList<DailyOffer>();
        for (int index = 0; index < DailyOfferState.BUY_OFFER_COUNT; index++) {
            duplicateBuys.add(new DailyOffer(find(TradePool.BUY_COMMON, "wheat_seeds").getCatalogKey(), true,
                    TradeCatalogEntry.UNLIMITED_STOCK));
        }
        List<DailyOffer> sales = new java.util.ArrayList<DailyOffer>();
        for (TradeCatalogEntry entry : TradeCatalog.getPool(TradePool.SELL_CORE)) {
            sales.add(new DailyOffer(entry.getCatalogKey(), true, TradeCatalogEntry.UNLIMITED_STOCK));
            if (sales.size() == DailyOfferState.SELL_OFFER_COUNT) {
                break;
            }
        }
        List<DailyOffer> uniqueBuys = new java.util.ArrayList<DailyOffer>();
        for (TradeCatalogEntry entry : TradeCatalog.getPool(TradePool.BUY_COMMON)) {
            uniqueBuys.add(new DailyOffer(entry.getCatalogKey(), true, entry.getInitialRemainingItems()));
            if (uniqueBuys.size() == DailyOfferState.BUY_OFFER_COUNT) {
                break;
            }
        }
        DailyOfferState currentState = new DailyOfferState(0L, sales, uniqueBuys);
        NBTTagCompound legacyTenSlotState = currentState.writeToNBT();
        legacyTenSlotState.getTagList("buyOffers", 10).appendTag(uniqueBuys.get(0).writeToNBT());
        check(DailyOfferState.readFromNBT(legacyTenSlotState) == null,
                "legacy ten-slot purchase state regenerates");

        List<DailyOffer> unorderedQualities = new java.util.ArrayList<DailyOffer>();
        unorderedQualities.add(new DailyOffer(find(TradePool.BUY_TREASURE, "nether_star").getCatalogKey(), true, 1));
        unorderedQualities.add(new DailyOffer(find(TradePool.BUY_COMMON, "wheat_seeds").getCatalogKey(), true,
                TradeCatalogEntry.UNLIMITED_STOCK));
        unorderedQualities.add(new DailyOffer(find(TradePool.BUY_RARE, "coal_ore").getCatalogKey(), true,
                find(TradePool.BUY_RARE, "coal_ore").getInitialRemainingItems()));
        unorderedQualities.add(new DailyOffer(find(TradePool.BUY_UNCOMMON, "gold_ingot").getCatalogKey(), true,
                find(TradePool.BUY_UNCOMMON, "gold_ingot").getInitialRemainingItems()));
        MerchantOfferService.sortPurchaseOffers(unorderedQualities);
        for (int index = 0; index < unorderedQualities.size(); index++) {
            check(MerchantOfferService.getPurchaseQualityRank(unorderedQualities.get(index)) == index,
                    "purchase offers sort from common to treasure");
        }
        try {
            new DailyOfferState(0L, sales, duplicateBuys);
            throw new AssertionError("duplicate daily purchase products must be rejected");
        } catch (IllegalArgumentException expected) {
            // Server-side state validation is the final uniqueness guard.
        }
        MerchantRecord merchantRecord = new MerchantRecord(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID());
        check(merchantRecord.updateKnownEntityChunk(-3, 7), "merchant entity chunk first observation");
        check(!merchantRecord.updateKnownEntityChunk(-3, 7), "merchant entity chunk duplicate observation");
        MerchantRecord restoredMerchantRecord = MerchantRecord.readFromNBT(merchantRecord.writeToNBT());
        check(restoredMerchantRecord.hasKnownEntityChunk() && restoredMerchantRecord.getKnownEntityChunkX() == -3
                && restoredMerchantRecord.getKnownEntityChunkZ() == 7, "merchant entity chunk persistence");
        check(find(TradePool.BUY_TREASURE, "nether_star").getProductIdentity(0)
                        .equals(find(TradePool.BUY_TREASURE, "nether_star").getProductIdentity(0)),
                "product identity is stable");
        System.out.println("merchantCatalogSelfTest PASS");
    }

    private static void verifyMerchantSnapshotRoundTrip() {
        List<MerchantTradeOfferView> sells = disabledViews(MerchantTradeSnapshot.SELL_COUNT);
        List<MerchantTradeOfferView> buys = disabledViews(MerchantTradeSnapshot.BUY_COUNT);
        MerchantTradeSnapshot snapshot = new MerchantTradeSnapshot(java.util.UUID.randomUUID(), 3, 0L, 0L, sells, buys);
        ByteBuf buffer = Unpooled.buffer();
        new SyncMerchantTradeMessage(snapshot).toBytes(buffer);
        SyncMerchantTradeMessage decodedMessage = new SyncMerchantTradeMessage();
        decodedMessage.fromBytes(buffer);
        MerchantTradeSnapshot decoded = decodedMessage.getSnapshot();
        check(decoded != null && decoded.getSellOffers().size() == MerchantTradeSnapshot.SELL_COUNT
                        && decoded.getBuyOffers().size() == MerchantTradeSnapshot.BUY_COUNT,
                "merchant snapshot uses the current 6 + 8 protocol counts");
    }

    private static List<MerchantTradeOfferView> disabledViews(int count) {
        List<MerchantTradeOfferView> views = new java.util.ArrayList<MerchantTradeOfferView>();
        for (int index = 0; index < count; index++) {
            views.add(new MerchantTradeOfferView(false, "", 0, 0L, null,
                    TradeCatalogEntry.UNLIMITED_STOCK, 0));
        }
        return views;
    }

    private static TradeCatalogEntry find(TradePool pool, String suffix) {
        for (TradeCatalogEntry entry : TradeCatalog.getPool(pool)) {
            if (entry.getCatalogKey().endsWith("/" + suffix)) {
                return entry;
            }
        }
        throw new AssertionError("Missing catalog entry: " + suffix);
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError(name);
        }
    }

    private static final class FixedRandom extends Random {
        private final int value;

        private FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return value;
        }
    }
}
