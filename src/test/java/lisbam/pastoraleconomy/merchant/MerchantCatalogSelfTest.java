package lisbam.pastoraleconomy.merchant;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import java.util.List;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Deterministic regression checks for the batch 08--11 merchant catalog. */
public final class MerchantCatalogSelfTest {
    private MerchantCatalogSelfTest() {
    }

    public static void main(String[] args) {
        Bootstrap.register();
        check(VillageService.getTargetMerchantCount(2) == 3, "2 villagers");
        check(VillageService.getTargetMerchantCount(15) == 3, "15 villagers");
        check(VillageService.getTargetMerchantCount(16) == 4, "16 villagers");
        check(VillageService.getTargetMerchantCount(20) == 4, "20 villagers");
        check(VillageService.getTargetMerchantCount(21) == 5, "21 villagers");
        check(VillageService.getTargetMerchantCount(35) == 7, "35 villagers");
        check(VillageService.getTargetMerchantCount(36) == 8, "36 villagers");
        check(VillageService.getTargetMerchantCount(100) == 20, "100 villagers");
        check(VillageService.getTargetMerchantCount(1000) == 200, "1000 villagers");

        check(TradeCatalog.getPool(TradePool.SELL_CORE).size() == 9, "core sell pool");
        check(TradeCatalog.getPool(TradePool.SELL_SECONDARY).size() == 13, "secondary sell pool");
        check(TradeCatalog.getPool(TradePool.BUY_COMMON).size() == 56, "common buy pool");
        check(TradeCatalog.getPool(TradePool.BUY_UNCOMMON).size() == 42, "uncommon buy pool");
        check(TradeCatalog.getPool(TradePool.BUY_RARE).size() == 28, "rare buy pool");
        check(TradeCatalog.getPool(TradePool.BUY_TREASURE).size() == 33, "treasure buy pool");

        String[][] rareStocks = {
                {"diamond", "16"}, {"emerald", "16"}, {"slime_ball", "8"}, {"blaze_rod", "16"}, {"ghast_tear", "16"},
                {"ender_pearl", "16"}, {"wither_skeleton_skull", "4"}, {"shulker_shell", "8"},
                {"dragon_breath", "16"}, {"sponge", "8"}, {"chainmail_helmet", "8"},
                {"chainmail_chestplate", "8"}, {"chainmail_leggings", "8"}, {"chainmail_boots", "8"},
                {"iron_horse_armor", "8"}, {"golden_horse_armor", "8"}, {"saddle", "8"},
                {"coal_ore", "8"}, {"iron_ore", "8"}, {"gold_ore", "8"}, {"redstone_ore", "8"},
                {"lapis_ore", "8"}, {"quartz_ore", "8"}, {"experience_bottle", "8"}, {"name_tag", "8"},
                {"skeleton_skull", "8"}, {"zombie_head", "8"}, {"creeper_head", "8"}
        };
        for (String[] row : rareStocks) {
            check(find(TradePool.BUY_RARE, row[0]).getInitialRemainingBundles() == Integer.parseInt(row[1]),
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
        check(gold.getInitialRemainingBundles() == 16 && gold.getBundleSize() == 4, "gold stock");
        check(lava.getInitialRemainingBundles() == 16 && lava.getBundleSize() == 1, "lava stock");
        check(magma.getInitialRemainingBundles() == 16 && magma.getBundleSize() == 4, "magma stock");
        check(rabbitFoot.getInitialRemainingBundles() == 16 && rabbitFoot.getBundleSize() == 1, "rabbit foot stock");

        Set<String> keys = new HashSet<String>();
        for (TradePool pool : TradePool.values()) {
            for (TradeCatalogEntry entry : TradeCatalog.getPool(pool)) {
                check(keys.add(entry.getCatalogKey()), "duplicate catalog key");
                check(entry.getBasePrice() > 0L, "positive base price");
                check(entry.getBundleSize() > 0, "positive bundle size");
                check(entry.getInitialRemainingBundles() > 0 || entry.isUnlimitedStock(), "valid stock");
                if (pool == TradePool.BUY_TREASURE) {
                    check(entry.getInitialRemainingBundles() == 1, "treasure stock is one");
                    check(Math.abs(entry.getVolatility() - 0.08D) < 0.0000001D, "treasure volatility");
                }
                if (pool == TradePool.BUY_RARE) {
                    check(!entry.isUnlimitedStock(), "rare stock is finite");
                    check(Math.abs(entry.getVolatility() - 0.18D) < 0.0000001D
                            || Math.abs(entry.getVolatility() - 0.12D) < 0.0000001D, "rare volatility");
                }
                ItemStack stack = entry.createStack(entry.getBundleSize(), entry.isEnchantment() ? 1 : 0);
                check(stack != null && !stack.isEmpty() && stack.getItem() != Items.AIR, "item exists");
                check(stack.getCount() == entry.getBundleSize(), "bundle output count");
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
        DailyOffer resolved = new DailyOffer(harvest.getCatalogKey(), true, 1, 3);
        DailyOffer restored = DailyOffer.readFromNBT(resolved.writeToNBT());
        check(restored.getEnchantmentLevel() == 3 && restored.getRemainingBundles() == 1,
                "resolved enchantment level persistence");
        NBTTagCompound oldPlaceholder = new DailyOffer("", false, TradeCatalogEntry.UNLIMITED_STOCK).writeToNBT();
        check(DailyOffer.readFromNBT(oldPlaceholder).getEnchantmentLevel() == 0, "legacy placeholder level");
        MerchantRecord merchantRecord = new MerchantRecord(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID());
        check(merchantRecord.updateKnownEntityChunk(-3, 7), "merchant entity chunk first observation");
        check(!merchantRecord.updateKnownEntityChunk(-3, 7), "merchant entity chunk duplicate observation");
        MerchantRecord restoredMerchantRecord = MerchantRecord.readFromNBT(merchantRecord.writeToNBT());
        check(restoredMerchantRecord.hasKnownEntityChunk() && restoredMerchantRecord.getKnownEntityChunkX() == -3
                && restoredMerchantRecord.getKnownEntityChunkZ() == 7, "merchant entity chunk persistence");
        verifyLegacySlimeBallMigration();
        System.out.println("merchantCatalogSelfTest PASS");
    }

    private static void verifyLegacySlimeBallMigration() {
        MerchantRecord merchant = new MerchantRecord(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID());
        List<DailyOffer> sell = new java.util.ArrayList<DailyOffer>();
        for (int index = 0; index < DailyOfferState.SELL_OFFER_COUNT; index++) {
            sell.add(new DailyOffer(find(TradePool.SELL_CORE, "wheat").getCatalogKey(), true,
                    TradeCatalogEntry.UNLIMITED_STOCK));
        }
        List<DailyOffer> buy = new java.util.ArrayList<DailyOffer>();
        for (int index = 0; index < 4; index++) {
            buy.add(new DailyOffer(find(TradePool.BUY_COMMON, "wheat_seeds").getCatalogKey(), true,
                    TradeCatalogEntry.UNLIMITED_STOCK));
        }
        buy.add(new DailyOffer(LisBamPastoralEconomy.MODID + ":merchant/slime_ball", true,
                TradeCatalogEntry.UNLIMITED_STOCK));
        buy.add(new DailyOffer(find(TradePool.BUY_UNCOMMON, "gold_ingot").getCatalogKey(), true, 16));
        buy.add(new DailyOffer(find(TradePool.BUY_UNCOMMON, "lava_bucket").getCatalogKey(), true, 16));
        buy.add(new DailyOffer(find(TradePool.BUY_RARE, "diamond").getCatalogKey(), true, 16));
        buy.add(new DailyOffer(find(TradePool.BUY_RARE, "emerald").getCatalogKey(), true, 16));
        buy.add(new DailyOffer(find(TradePool.BUY_TREASURE, "nether_star").getCatalogKey(), true, 1));
        merchant.setDailyOfferState(new DailyOfferState(42L, sell, buy));

        check(MerchantOfferService.migrateLegacyUncommonSlimeBallOffer(merchant, merchant.getDailyOfferState()),
                "legacy uncommon slime ball must migrate");
        DailyOffer replacement = merchant.getDailyOfferState().getBuyOffer(4);
        TradeCatalogEntry entry = TradeCatalog.get(replacement.getCatalogKey());
        check(entry != null && entry.getPool() == TradePool.BUY_UNCOMMON,
                "legacy slime ball slot must receive an uncommon offer");
        check(!LisBamPastoralEconomy.MODID.concat(":merchant/slime_ball").equals(replacement.getCatalogKey()),
                "legacy slime ball must not remain in the uncommon slot");
        check(merchant.getDailyOfferState().getBuyOffer(7).getCatalogKey()
                        .equals(find(TradePool.BUY_RARE, "diamond").getCatalogKey()),
                "legacy migration must preserve rare offers");
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
