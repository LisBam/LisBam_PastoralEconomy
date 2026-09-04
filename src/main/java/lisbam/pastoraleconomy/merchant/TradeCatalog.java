package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import lisbam.pastoraleconomy.market.MarketCatalog;
import lisbam.pastoraleconomy.market.MarketCommodity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Enchantments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Batches 08--11's only merchant catalog. MarketCatalog remains the sole
 * pricing directory; this class adds offer-pool, bundle, stock, and matcher
 * information without scattering item rules across GUI or packet code.
 */
public final class TradeCatalog {
    private static final Map<String, TradeCatalogEntry> BY_KEY;
    private static final Map<TradePool, List<TradeCatalogEntry>> BY_POOL;

    static {
        Map<String, TradeCatalogEntry> definitions = new LinkedHashMap<String, TradeCatalogEntry>();
        addSellDefinitions(definitions);
        addCommonBuyDefinitions(definitions);
        addUncommonBuyDefinitions(definitions);
        addRareBuyDefinitions(definitions);
        addTreasureDefinitions(definitions);
        BY_KEY = Collections.unmodifiableMap(definitions);

        Map<TradePool, List<TradeCatalogEntry>> poolEntries = new EnumMap<TradePool, List<TradeCatalogEntry>>(TradePool.class);
        for (TradePool pool : TradePool.values()) {
            poolEntries.put(pool, new ArrayList<TradeCatalogEntry>());
        }
        for (TradeCatalogEntry entry : definitions.values()) {
            poolEntries.get(entry.getPool()).add(entry);
        }
        for (TradePool pool : TradePool.values()) {
            poolEntries.put(pool, Collections.unmodifiableList(poolEntries.get(pool)));
        }
        BY_POOL = Collections.unmodifiableMap(poolEntries);
    }

    private TradeCatalog() {
    }

    public static TradeCatalogEntry get(String key) {
        return BY_KEY.get(key);
    }

    public static List<TradeCatalogEntry> getPool(TradePool pool) {
        List<TradeCatalogEntry> entries = BY_POOL.get(pool);
        return entries == null ? Collections.<TradeCatalogEntry>emptyList() : entries;
    }

    private static void addSellDefinitions(Map<String, TradeCatalogEntry> definitions) {
        add(definitions, "wheat", "sell/crop/wheat", TradePool.SELL_CORE, 1, TradeCatalogEntry.UNLIMITED_STOCK, 5);
        add(definitions, "carrot", "sell/crop/carrot", TradePool.SELL_CORE, 1, TradeCatalogEntry.UNLIMITED_STOCK, 5);
        add(definitions, "potato", "sell/crop/potato", TradePool.SELL_CORE, 1, TradeCatalogEntry.UNLIMITED_STOCK, 4);
        add(definitions, "beetroot", "sell/crop/beetroot", TradePool.SELL_CORE, 1, TradeCatalogEntry.UNLIMITED_STOCK, 7);
        add(definitions, "pumpkin", "sell/crop/pumpkin", TradePool.SELL_CORE, 1, TradeCatalogEntry.UNLIMITED_STOCK, 17);
        add(definitions, "melon_block", "sell/crop/melon_block", TradePool.SELL_CORE, 1, TradeCatalogEntry.UNLIMITED_STOCK, 25);
        add(definitions, "melon_slice", "sell/crop/melon_slice", TradePool.SELL_CORE, 1, TradeCatalogEntry.UNLIMITED_STOCK, 3);
        add(definitions, "sugar_cane", "sell/crop/sugar_cane", TradePool.SELL_CORE, 1, TradeCatalogEntry.UNLIMITED_STOCK, 4);
        add(definitions, "cactus", "sell/crop/cactus", TradePool.SELL_CORE, 1, TradeCatalogEntry.UNLIMITED_STOCK, 3);

        add(definitions, "cocoa_beans", "sell/crop/cocoa_beans", TradePool.SELL_SECONDARY, 1, TradeCatalogEntry.UNLIMITED_STOCK, 5);
        add(definitions, "red_mushroom", "sell/crop/red_mushroom", TradePool.SELL_SECONDARY, 1, TradeCatalogEntry.UNLIMITED_STOCK, 5);
        add(definitions, "brown_mushroom", "sell/crop/brown_mushroom", TradePool.SELL_SECONDARY, 1, TradeCatalogEntry.UNLIMITED_STOCK, 5);
        add(definitions, "apple", "sell/agriculture/apple", TradePool.SELL_SECONDARY, 1, TradeCatalogEntry.UNLIMITED_STOCK, 10);
        add(definitions, "egg", "sell/livestock/egg", TradePool.SELL_SECONDARY, 1, TradeCatalogEntry.UNLIMITED_STOCK, 6);
        add(definitions, "feather", "sell/livestock/feather", TradePool.SELL_SECONDARY, 1, TradeCatalogEntry.UNLIMITED_STOCK, 5);
        add(definitions, "leather", "sell/livestock/leather", TradePool.SELL_SECONDARY, 1, TradeCatalogEntry.UNLIMITED_STOCK, 12);
        add(definitions, "rabbit_hide", "sell/livestock/rabbit_hide", TradePool.SELL_SECONDARY, 1, TradeCatalogEntry.UNLIMITED_STOCK, 6);
        add(definitions, "rabbit_foot", "sell/livestock/rabbit_foot", TradePool.SELL_SECONDARY, 1, TradeCatalogEntry.UNLIMITED_STOCK, 20);
        // Previous project market data explicitly places these two source-ambiguous crops in the secondary pool.
        add(definitions, "nether_wart", "sell/crop/nether_wart", TradePool.SELL_SECONDARY, 1, TradeCatalogEntry.UNLIMITED_STOCK, 8);
        add(definitions, "chorus_fruit", "sell/crop/chorus_fruit", TradePool.SELL_SECONDARY, 1, TradeCatalogEntry.UNLIMITED_STOCK, 8);
        add(definitions, "milk_bucket", "sell/livestock/milk_bucket", TradePool.SELL_SECONDARY, 1, TradeCatalogEntry.UNLIMITED_STOCK, 20);
        addAnyWool(definitions, "wool", "sell/livestock/wool", TradePool.SELL_SECONDARY, 8);
    }

    private static void addCommonBuyDefinitions(Map<String, TradeCatalogEntry> definitions) {
        String[][] rows = {
                {"wheat_seeds", "buy/common/wheat_seeds", "16", "80"}, {"carrot_buy", "buy/common/carrot", "8", "80"},
                {"potato_buy", "buy/common/potato", "8", "80"}, {"beetroot_seeds", "buy/common/beetroot_seeds", "16", "96"},
                {"pumpkin_seeds", "buy/common/pumpkin_seeds", "8", "120"}, {"melon_seeds", "buy/common/melon_seeds", "8", "120"},
                {"cocoa_beans_buy", "buy/common/cocoa_beans", "8", "120"}, {"sugar_cane_buy", "buy/common/sugar_cane", "8", "80"},
                {"cactus_buy", "buy/common/cactus", "8", "80"}, {"red_mushroom_buy", "buy/common/red_mushroom", "8", "120"},
                {"brown_mushroom_buy", "buy/common/brown_mushroom", "8", "120"}, {"bone_meal", "buy/common/bone_meal", "16", "180"},
                {"nether_wart_buy", "buy/common/nether_wart", "4", "400"}, {"chorus_flower", "buy/common/chorus_flower", "1", "500"},
                {"sapling_oak", "buy/common/sapling_oak", "4", "120"}, {"sapling_spruce", "buy/common/sapling_spruce", "4", "120"},
                {"sapling_birch", "buy/common/sapling_birch", "4", "120"}, {"sapling_jungle", "buy/common/sapling_jungle", "4", "120"},
                {"sapling_acacia", "buy/common/sapling_acacia", "4", "120"}, {"sapling_dark_oak", "buy/common/sapling_dark_oak", "4", "120"},
                {"log_oak", "buy/common/log_oak", "8", "100"}, {"log_spruce", "buy/common/log_spruce", "8", "100"},
                {"log_birch", "buy/common/log_birch", "8", "100"}, {"log_jungle", "buy/common/log_jungle", "8", "100"},
                {"log_acacia", "buy/common/log_acacia", "8", "100"}, {"log_dark_oak", "buy/common/log_dark_oak", "8", "100"},
                {"dirt", "buy/common/dirt", "16", "32"}, {"cobblestone", "buy/common/cobblestone", "16", "48"},
                {"stone", "buy/common/stone", "16", "64"}, {"granite", "buy/common/granite", "16", "96"},
                {"andesite", "buy/common/andesite", "16", "96"}, {"diorite", "buy/common/diorite", "16", "96"},
                {"sand", "buy/common/sand", "16", "96"}, {"red_sand", "buy/common/red_sand", "16", "128"},
                {"gravel", "buy/common/gravel", "16", "80"}, {"clay_ball", "buy/common/clay_ball", "16", "96"},
                {"glass", "buy/common/glass", "16", "160"}, {"ice", "buy/common/ice", "8", "80"},
                {"packed_ice", "buy/common/packed_ice", "8", "320"}, {"snow", "buy/common/snow", "16", "64"},
                {"obsidian", "buy/common/obsidian", "4", "400"}, {"coal", "buy/common/coal", "8", "160"},
                {"charcoal", "buy/common/charcoal", "8", "160"}, {"iron_ingot", "buy/common/iron_ingot", "4", "360"},
                {"redstone", "buy/common/redstone", "8", "240"}, {"lapis_lazuli", "buy/common/lapis_lazuli", "8", "280"},
                {"flint", "buy/common/flint", "8", "160"}, {"bone", "buy/common/bone", "8", "240"},
                {"string", "buy/common/string", "8", "240"}, {"spider_eye", "buy/common/spider_eye", "4", "160"},
                {"rotten_flesh", "buy/common/rotten_flesh", "8", "160"}, {"ink_sac", "buy/common/ink_sac", "8", "160"},
                {"netherrack", "buy/common/netherrack", "16", "96"}, {"soul_sand", "buy/common/soul_sand", "8", "200"},
                {"egg_buy", "buy/common/egg", "8", "80"}, {"feather_buy", "buy/common/feather", "8", "120"}
        };
        addRows(definitions, TradePool.BUY_COMMON, TradeCatalogEntry.UNLIMITED_STOCK, rows);
    }

    private static void addUncommonBuyDefinitions(Map<String, TradeCatalogEntry> definitions) {
        String[][] rows = {
                {"gold_ingot", "buy/rare/gold_ingot", "4", "800", "16"}, {"lava_bucket", "buy/rare/lava_bucket", "1", "600", "16"},
                {"gunpowder", "buy/rare/gunpowder", "8", "400"}, {"slime_ball", "buy/rare/slime_ball", "4", "480"},
                {"magma_cream", "buy/rare/magma_cream", "4", "640", "16"}, {"glowstone_dust", "buy/rare/glowstone_dust", "8", "480"},
                {"nether_quartz", "buy/rare/nether_quartz", "8", "480"}, {"end_stone", "buy/rare/end_stone", "16", "320"},
                {"prismarine_shard", "buy/rare/prismarine_shard", "8", "400"}, {"prismarine_crystals", "buy/rare/prismarine_crystals", "4", "400"},
                {"leather_buy", "buy/rare/leather", "4", "200"}, {"rabbit_hide_buy", "buy/rare/rabbit_hide", "4", "160"},
                {"rabbit_foot_buy", "buy/rare/rabbit_foot", "1", "400", "16"}, {"milk_bucket_buy", "buy/rare/milk_bucket", "1", "180"},
                {"beef", "buy/rare/beef", "4", "200"}, {"porkchop", "buy/rare/porkchop", "4", "200"},
                {"chicken", "buy/rare/chicken", "4", "160"}, {"mutton", "buy/rare/mutton", "4", "200"},
                {"rabbit", "buy/rare/rabbit", "4", "240"}, {"fish", "buy/rare/fish", "4", "160"},
                {"salmon", "buy/rare/salmon", "4", "200"}, {"clownfish", "buy/rare/clownfish", "4", "240"},
                {"pufferfish", "buy/rare/pufferfish", "4", "240"}, {"cobweb", "buy/rare/cobweb", "4", "300"},
                {"dandelion", "buy/rare/dandelion", "4", "120"}, {"poppy", "buy/rare/poppy", "4", "120"},
                {"blue_orchid", "buy/rare/blue_orchid", "4", "160"}, {"allium", "buy/rare/allium", "4", "160"},
                {"azure_bluet", "buy/rare/azure_bluet", "4", "160"}, {"red_tulip", "buy/rare/red_tulip", "4", "160"},
                {"orange_tulip", "buy/rare/orange_tulip", "4", "160"}, {"white_tulip", "buy/rare/white_tulip", "4", "160"},
                {"pink_tulip", "buy/rare/pink_tulip", "4", "160"}, {"oxeye_daisy", "buy/rare/oxeye_daisy", "4", "160"},
                {"sunflower", "buy/rare/sunflower", "2", "160"}, {"lilac", "buy/rare/lilac", "2", "160"},
                {"rose_bush", "buy/rare/rose_bush", "2", "160"}, {"peony", "buy/rare/peony", "2", "160"},
                {"vine", "buy/rare/vine", "8", "160"}, {"podzol", "buy/rare/podzol", "8", "160"},
                {"mycelium", "buy/rare/mycelium", "8", "240"}, {"waterlily", "buy/rare/waterlily", "4", "160"},
                {"dead_bush", "buy/rare/dead_bush", "4", "120"}
        };
        for (String[] row : rows) {
            int stock = row.length == 5 ? Integer.parseInt(row[4]) : TradeCatalogEntry.UNLIMITED_STOCK;
            add(definitions, row[0], row[1], TradePool.BUY_UNCOMMON, Integer.parseInt(row[2]), stock,
                    Long.parseLong(row[3]));
        }
    }

    private static void addRareBuyDefinitions(Map<String, TradeCatalogEntry> definitions) {
        String[][] rows = {
                {"diamond", "1", "1600", "16", "0.18"}, {"emerald", "1", "700", "16", "0.18"},
                {"blaze_rod", "1", "900", "16", "0.18"}, {"ghast_tear", "1", "1200", "16", "0.18"},
                {"ender_pearl", "1", "800", "16", "0.18"}, {"wither_skeleton_skull", "1", "4000", "4", "0.18"},
                {"shulker_shell", "1", "5000", "8", "0.12"}, {"dragon_breath", "1", "1800", "16", "0.12"},
                {"sponge", "1", "5000", "8", "0.12"}, {"chainmail_helmet", "1", "1500", "8", "0.12"},
                {"chainmail_chestplate", "1", "2500", "8", "0.12"}, {"chainmail_leggings", "1", "2200", "8", "0.12"},
                {"chainmail_boots", "1", "1200", "8", "0.12"}, {"iron_horse_armor", "1", "2000", "8", "0.12"},
                {"golden_horse_armor", "1", "2500", "8", "0.12"}, {"saddle", "1", "1800", "8", "0.12"},
                {"coal_ore", "4", "400", "8", "0.12"}, {"iron_ore", "4", "500", "8", "0.12"},
                {"gold_ore", "4", "1000", "8", "0.12"}, {"redstone_ore", "4", "800", "8", "0.12"},
                {"lapis_ore", "4", "900", "8", "0.12"}, {"quartz_ore", "4", "700", "8", "0.12"},
                {"experience_bottle", "4", "1200", "8", "0.12"}, {"name_tag", "1", "1000", "8", "0.12"},
                {"skeleton_skull", "1", "2500", "8", "0.12"}, {"zombie_head", "1", "2500", "8", "0.12"},
                {"creeper_head", "1", "3500", "8", "0.12"}
        };
        for (String[] row : rows) {
            addChecked(definitions, row[0], "buy/rare/" + row[0], TradePool.BUY_RARE,
                    Integer.parseInt(row[1]), Integer.parseInt(row[3]), Long.parseLong(row[2]),
                    Double.parseDouble(row[4]));
        }
    }

    private static void addTreasureDefinitions(Map<String, TradeCatalogEntry> definitions) {
        String[][] rows = {
                {"nether_star", "12000"}, {"enchanted_golden_apple", "25000"}, {"elytra", "120000"},
                {"dragon_head", "10000"}, {"dragon_egg", "250000"}, {"diamond_horse_armor", "6000"},
                {"diamond_ore", "3000"}, {"emerald_ore", "2500"}, {"totem_of_undying", "10000"},
                {"record_13", "5000"}, {"record_cat", "5000"}, {"record_blocks", "5000"},
                {"record_chirp", "5000"}, {"record_far", "5000"}, {"record_mall", "5000"},
                {"record_mellohi", "5000"}, {"record_stal", "5000"}, {"record_strad", "5000"},
                {"record_ward", "5000"}, {"record_11", "5000"}, {"record_wait", "5000"}
        };
        for (String[] row : rows) {
            addChecked(definitions, row[0], "buy/treasure/" + row[0], TradePool.BUY_TREASURE,
                    1, 1, Long.parseLong(row[1]), 0.08D);
        }

        addEnchantment(definitions, "mending", Enchantments.MENDING, new int[]{100},
                new long[]{12000}, new String[]{"buy/treasure/enchanted_book/mending_1"});
        addEnchantment(definitions, "frost_walker", Enchantments.FROST_WALKER, new int[]{65, 35},
                new long[]{5000, 8000}, new String[]{"buy/treasure/enchanted_book/frost_walker_1",
                        "buy/treasure/enchanted_book/frost_walker_2"});
        addEnchantment(definitions, "binding_curse", Enchantments.BINDING_CURSE, new int[]{100},
                new long[]{2000}, new String[]{"buy/treasure/enchanted_book/binding_curse_1"});
        addEnchantment(definitions, "vanishing_curse", Enchantments.VANISHING_CURSE, new int[]{100},
                new long[]{2000}, new String[]{"buy/treasure/enchanted_book/vanishing_curse_1"});
        addEnchantment(definitions, "harvest", ModEnchantments.HARVEST, new int[]{50, 35, 15},
                new long[]{5000, 8000, 12000}, new String[]{"buy/treasure/enchanted_book/harvest_1",
                        "buy/treasure/enchanted_book/harvest_2", "buy/treasure/enchanted_book/harvest_3"});
        addEnchantment(definitions, "farmland_walker", ModEnchantments.FARMLAND_WALKER, new int[]{50, 35, 15},
                new long[]{3500, 5500, 8000}, new String[]{"buy/treasure/enchanted_book/tiller_1",
                        "buy/treasure/enchanted_book/tiller_2", "buy/treasure/enchanted_book/tiller_3"});
        addEnchantment(definitions, "pastoral_favor", ModEnchantments.PASTORAL_FAVOR, new int[]{45, 30, 18, 7},
                new long[]{3000, 5000, 8000, 12000}, new String[]{"buy/treasure/enchanted_book/pastoral_favor_1",
                        "buy/treasure/enchanted_book/pastoral_favor_2", "buy/treasure/enchanted_book/pastoral_favor_3",
                        "buy/treasure/enchanted_book/pastoral_favor_4"});
        addEnchantment(definitions, "fine_cultivation", ModEnchantments.FINE_CULTIVATION, new int[]{45, 30, 18, 7},
                new long[]{4000, 6000, 9000, 13000}, new String[]{"buy/treasure/enchanted_book/intensive_farming_1",
                        "buy/treasure/enchanted_book/intensive_farming_2", "buy/treasure/enchanted_book/intensive_farming_3",
                        "buy/treasure/enchanted_book/intensive_farming_4"});
        addEnchantment(definitions, "felling", ModEnchantments.FELLING, new int[]{100},
                new long[]{10000}, new String[]{"buy/treasure/enchanted_book/lumbering_1"});
        addEnchantment(definitions, "slaughter", ModEnchantments.SLAUGHTER, new int[]{50, 35, 15},
                new long[]{6000, 10000, 15000}, new String[]{"buy/treasure/enchanted_book/butchering_1",
                        "buy/treasure/enchanted_book/butchering_2", "buy/treasure/enchanted_book/butchering_3"});
        addEnchantment(definitions, "fleetfoot", ModEnchantments.FLEETFOOT, new int[]{45, 30, 18, 7},
                new long[]{5000, 8000, 12000, 18000}, new String[]{"buy/treasure/enchanted_book/swift_footed_1",
                        "buy/treasure/enchanted_book/swift_footed_2", "buy/treasure/enchanted_book/swift_footed_3",
                        "buy/treasure/enchanted_book/swift_footed_4"});
        addEnchantment(definitions, "night_vision", ModEnchantments.NIGHT_VISION, new int[]{100},
                new long[]{12000}, new String[]{"buy/treasure/enchanted_book/night_vision_1"});
    }

    private static void addEnchantment(Map<String, TradeCatalogEntry> definitions, String name, Enchantment enchantment,
                                       int[] weights, long[] prices, String[] marketKeys) {
        String catalogKey = LisBamPastoralEconomy.MODID + ":merchant/treasure_enchant_" + name;
        String[] fullMarketKeys = new String[marketKeys.length];
        for (int index = 0; index < marketKeys.length; index++) {
            fullMarketKeys[index] = LisBamPastoralEconomy.MODID + ":" + marketKeys[index];
        }
        EnchantmentTradeDefinition definition = new EnchantmentTradeDefinition(catalogKey, enchantment, weights,
                prices, fullMarketKeys);
        if (definitions.containsKey(catalogKey)) {
            throw new IllegalStateException("Duplicate merchant catalog key: " + catalogKey);
        }
        definitions.put(catalogKey, new TradeCatalogEntry(catalogKey, fullMarketKeys[0], TradePool.BUY_TREASURE,
                1, 1, false, definition.getCommodity(1), definition));
    }

    private static void addChecked(Map<String, TradeCatalogEntry> definitions, String path, String marketPath,
                                   TradePool pool, int bundleSize, int stock, long expectedBasePrice,
                                   double expectedVolatility) {
        add(definitions, path, marketPath, pool, bundleSize, stock, expectedBasePrice);
        TradeCatalogEntry entry = definitions.get(LisBamPastoralEconomy.MODID + ":merchant/" + path);
        if (entry == null || Math.abs(entry.getVolatility() - expectedVolatility) > 0.0000001D) {
            throw new IllegalStateException("Merchant catalog volatility definition is missing or changed: " + path);
        }
    }

    private static void addRows(Map<String, TradeCatalogEntry> definitions, TradePool pool, int stock, String[][] rows) {
        for (String[] row : rows) {
            add(definitions, row[0], row[1], pool, Integer.parseInt(row[2]), stock, Long.parseLong(row[3]));
        }
    }

    private static void add(Map<String, TradeCatalogEntry> definitions, String path, String marketPath, TradePool pool,
                            int bundleSize, int stock, long expectedBasePrice) {
        String catalogKey = LisBamPastoralEconomy.MODID + ":merchant/" + path;
        String marketKey = LisBamPastoralEconomy.MODID + ":" + marketPath;
        MarketCommodity commodity = MarketCatalog.get(marketKey);
        if (commodity == null || commodity.getBasePrice() != expectedBasePrice) {
            throw new IllegalStateException("Merchant catalog price definition is missing or changed: " + marketKey);
        }
        TradeCatalogEntry previous = definitions.put(catalogKey,
                new TradeCatalogEntry(catalogKey, marketKey, pool, bundleSize, stock, false, commodity));
        if (previous != null) {
            throw new IllegalStateException("Duplicate merchant catalog key: " + catalogKey);
        }
    }

    private static void addAnyWool(Map<String, TradeCatalogEntry> definitions, String path, String marketPath,
                                   TradePool pool, long expectedBasePrice) {
        String catalogKey = LisBamPastoralEconomy.MODID + ":merchant/" + path;
        String marketKey = LisBamPastoralEconomy.MODID + ":" + marketPath;
        MarketCommodity commodity = MarketCatalog.get(marketKey);
        if (commodity == null || commodity.getBasePrice() != expectedBasePrice) {
            throw new IllegalStateException("Merchant wool price definition is missing or changed.");
        }
        definitions.put(catalogKey, new TradeCatalogEntry(catalogKey, marketKey, pool, 1,
                TradeCatalogEntry.UNLIMITED_STOCK, true, commodity));
    }
}
