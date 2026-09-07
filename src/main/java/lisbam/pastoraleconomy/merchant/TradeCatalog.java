package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.enchantment.ModEnchantments;
import lisbam.pastoraleconomy.market.MarketCatalog;
import lisbam.pastoraleconomy.market.MarketCommodity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Batches 08--11's only merchant catalog. MarketCatalog remains the sole
 * pricing directory; this class adds offer-pool, item stock, and matcher
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

    /** Finds one of the 29 globally priced player-to-merchant purchase goods. */
    @Nullable
    public static TradeCatalogEntry findSellEntry(ItemStack stack) {
        for (TradeCatalogEntry entry : getPool(TradePool.SELL_CORE)) {
            if (entry.matches(stack)) {
                return entry;
            }
        }
        for (TradeCatalogEntry entry : getPool(TradePool.SELL_SECONDARY)) {
            if (entry.matches(stack)) {
                return entry;
            }
        }
        return null;
    }

    /** Recreates the native enchanted-book NBT for one market catalogue key. */
    @Nullable
    public static ItemStack createEnchantedBookStackForMarketKey(String marketKey) {
        if (marketKey == null || marketKey.isEmpty()) {
            return null;
        }
        for (TradeCatalogEntry entry : BY_KEY.values()) {
            if (!entry.isEnchantment()) {
                continue;
            }
            EnchantmentTradeDefinition definition = entry.getEnchantmentDefinition();
            for (int level = 1; level <= definition.getMaxLevel(); level++) {
                if (marketKey.equals(definition.getMarketKey(level))) {
                    return definition.createBookStack(1, level);
                }
            }
        }
        return null;
    }

    private static void addSellDefinitions(Map<String, TradeCatalogEntry> definitions) {
        add(definitions, "wheat", "sell/crop/wheat", TradePool.SELL_CORE, TradeCatalogEntry.UNLIMITED_STOCK, 50);
        add(definitions, "carrot", "sell/crop/carrot", TradePool.SELL_CORE, TradeCatalogEntry.UNLIMITED_STOCK, 60);
        add(definitions, "potato", "sell/crop/potato", TradePool.SELL_CORE, TradeCatalogEntry.UNLIMITED_STOCK, 60);
        add(definitions, "beetroot", "sell/crop/beetroot", TradePool.SELL_CORE, TradeCatalogEntry.UNLIMITED_STOCK, 50);
        add(definitions, "pumpkin", "sell/crop/pumpkin", TradePool.SELL_CORE, TradeCatalogEntry.UNLIMITED_STOCK, 125);
        add(definitions, "melon_block", "sell/crop/melon_block", TradePool.SELL_CORE, TradeCatalogEntry.UNLIMITED_STOCK, 150);
        add(definitions, "melon_slice", "sell/crop/melon_slice", TradePool.SELL_CORE, TradeCatalogEntry.UNLIMITED_STOCK, 30);
        add(definitions, "sugar_cane", "sell/crop/sugar_cane", TradePool.SELL_CORE, TradeCatalogEntry.UNLIMITED_STOCK, 35);
        add(definitions, "cactus", "sell/crop/cactus", TradePool.SELL_CORE, TradeCatalogEntry.UNLIMITED_STOCK, 40);

        add(definitions, "cocoa_beans", "sell/crop/cocoa_beans", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 75);
        add(definitions, "red_mushroom", "sell/crop/red_mushroom", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 50);
        add(definitions, "brown_mushroom", "sell/crop/brown_mushroom", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 50);
        add(definitions, "apple", "sell/agriculture/apple", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 90);
        add(definitions, "egg", "sell/livestock/egg", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 60);
        add(definitions, "feather", "sell/livestock/feather", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 50);
        add(definitions, "leather", "sell/livestock/leather", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 100);
        add(definitions, "rabbit_hide", "sell/livestock/rabbit_hide", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 175);
        add(definitions, "rabbit_foot", "sell/livestock/rabbit_foot", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 250);
        // Previous project market data explicitly places these two source-ambiguous crops in the secondary pool.
        add(definitions, "nether_wart", "sell/crop/nether_wart", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 100);
        add(definitions, "chorus_fruit", "sell/crop/chorus_fruit", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 150);
        add(definitions, "milk_bucket", "sell/livestock/milk_bucket", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 150);
        add(definitions, "beef_sell", "sell/livestock/beef", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 120);
        add(definitions, "porkchop_sell", "sell/livestock/porkchop", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 120);
        add(definitions, "chicken_sell", "sell/livestock/chicken", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 100);
        add(definitions, "mutton_sell", "sell/livestock/mutton", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 90);
        add(definitions, "rabbit_sell", "sell/livestock/rabbit", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 140);
        add(definitions, "fish_sell", "sell/livestock/fish", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 80);
        add(definitions, "salmon_sell", "sell/livestock/salmon", TradePool.SELL_SECONDARY, TradeCatalogEntry.UNLIMITED_STOCK, 100);
        addAnyWool(definitions, "wool", "sell/livestock/wool", TradePool.SELL_SECONDARY, 70);
    }

    private static void addCommonBuyDefinitions(Map<String, TradeCatalogEntry> definitions) {
        String[][] rows = {
                {"wheat_seeds", "buy/common/wheat_seeds", "800"}, {"carrot_buy", "buy/common/carrot", "800"},
                {"potato_buy", "buy/common/potato", "800"}, {"beetroot_seeds", "buy/common/beetroot_seeds", "1600"},
                {"pumpkin_seeds", "buy/common/pumpkin_seeds", "3200"}, {"melon_seeds", "buy/common/melon_seeds", "3200"},
                {"cocoa_beans_buy", "buy/common/cocoa_beans", "6400"}, {"sugar_cane_buy", "buy/common/sugar_cane", "3200"},
                {"cactus_buy", "buy/common/cactus", "3200"}, {"red_mushroom_buy", "buy/common/red_mushroom", "3200"},
                {"brown_mushroom_buy", "buy/common/brown_mushroom", "3200"}, {"bone_meal", "buy/common/bone_meal", "400"},
                {"nether_wart_buy", "buy/common/nether_wart", "9000"}, {"chorus_flower", "buy/common/chorus_flower", "12500"},
                {"sapling_oak", "buy/common/sapling_oak", "1200"}, {"sapling_spruce", "buy/common/sapling_spruce", "1200"},
                {"sapling_birch", "buy/common/sapling_birch", "1200"}, {"sapling_jungle", "buy/common/sapling_jungle", "1200"},
                {"sapling_acacia", "buy/common/sapling_acacia", "1200"}, {"sapling_dark_oak", "buy/common/sapling_dark_oak", "1200"},
                {"log_oak", "buy/common/log_oak", "1000"}, {"log_spruce", "buy/common/log_spruce", "1000"},
                {"log_birch", "buy/common/log_birch", "1000"}, {"log_jungle", "buy/common/log_jungle", "1000"},
                {"log_acacia", "buy/common/log_acacia", "1000"}, {"log_dark_oak", "buy/common/log_dark_oak", "1000"},
                {"dirt", "buy/common/dirt", "160"}, {"cobblestone", "buy/common/cobblestone", "240"},
                {"stone", "buy/common/stone", "320"}, {"granite", "buy/common/granite", "140"},
                {"andesite", "buy/common/andesite", "140"}, {"diorite", "buy/common/diorite", "140"},
                {"sand", "buy/common/sand", "200"}, {"red_sand", "buy/common/red_sand", "225"},
                {"gravel", "buy/common/gravel", "200"}, {"clay_ball", "buy/common/clay_ball", "225"},
                {"glass", "buy/common/glass", "400"}, {"ice", "buy/common/ice", "600"},
                {"packed_ice", "buy/common/packed_ice", "1600"}, {"snow", "buy/common/snow", "600"},
                {"obsidian", "buy/common/obsidian", "2000"}, {"coal", "buy/common/coal", "1600"},
                {"charcoal", "buy/common/charcoal", "1600"}, {"iron_ingot", "buy/common/iron_ingot", "900"},
                {"redstone", "buy/common/redstone", "1000"}, {"lapis_lazuli", "buy/common/lapis_lazuli", "800"},
                {"flint", "buy/common/flint", "800"}, {"bone", "buy/common/bone", "1000"},
                {"string", "buy/common/string", "400"}, {"spider_eye", "buy/common/spider_eye", "3200"},
                {"rotten_flesh", "buy/common/rotten_flesh", "1600"}, {"ink_sac", "buy/common/ink_sac", "1600"},
                {"netherrack", "buy/common/netherrack", "400"}, {"soul_sand", "buy/common/soul_sand", "1600"},
                {"egg_buy", "buy/common/egg", "800"}, {"feather_buy", "buy/common/feather", "1200"}
        };
        addRows(definitions, TradePool.BUY_COMMON, TradeCatalogEntry.UNLIMITED_STOCK, rows);
    }

    private static void addUncommonBuyDefinitions(Map<String, TradeCatalogEntry> definitions) {
        String[][] rows = {
                {"gold_ingot", "buy/rare/gold_ingot", "3200", "16"}, {"lava_bucket", "buy/rare/lava_bucket", "6000", "16"},
                {"gunpowder", "buy/rare/gunpowder", "1600"},
                {"magma_cream", "buy/rare/magma_cream", "6400", "16"}, {"glowstone_dust", "buy/rare/glowstone_dust", "200"},
                {"nether_quartz", "buy/rare/nether_quartz", "1600"}, {"end_stone", "buy/rare/end_stone", "3200"},
                {"prismarine_shard", "buy/rare/prismarine_shard", "2000"}, {"prismarine_crystals", "buy/rare/prismarine_crystals", "2000"},
                {"leather_buy", "buy/rare/leather", "2000"}, {"rabbit_hide_buy", "buy/rare/rabbit_hide", "1600"},
                {"rabbit_foot_buy", "buy/rare/rabbit_foot", "4000", "16"}, {"milk_bucket_buy", "buy/rare/milk_bucket", "1800"},
                {"beef", "buy/rare/beef", "2000"}, {"porkchop", "buy/rare/porkchop", "2000"},
                {"chicken", "buy/rare/chicken", "1600"}, {"mutton", "buy/rare/mutton", "2000"},
                {"rabbit", "buy/rare/rabbit", "2400"}, {"fish", "buy/rare/fish", "1600"},
                {"salmon", "buy/rare/salmon", "2000"}, {"clownfish", "buy/rare/clownfish", "2400"},
                {"pufferfish", "buy/rare/pufferfish", "2400"}, {"cobweb", "buy/rare/cobweb", "3000"},
                {"dandelion", "buy/rare/dandelion", "1200"}, {"poppy", "buy/rare/poppy", "1200"},
                {"blue_orchid", "buy/rare/blue_orchid", "1600"}, {"allium", "buy/rare/allium", "1600"},
                {"azure_bluet", "buy/rare/azure_bluet", "1600"}, {"red_tulip", "buy/rare/red_tulip", "1600"},
                {"orange_tulip", "buy/rare/orange_tulip", "1600"}, {"white_tulip", "buy/rare/white_tulip", "1600"},
                {"pink_tulip", "buy/rare/pink_tulip", "1600"}, {"oxeye_daisy", "buy/rare/oxeye_daisy", "1600"},
                {"sunflower", "buy/rare/sunflower", "1600"}, {"lilac", "buy/rare/lilac", "1600"},
                {"rose_bush", "buy/rare/rose_bush", "1600"}, {"peony", "buy/rare/peony", "1600"},
                {"vine", "buy/rare/vine", "1600"}, {"podzol", "buy/rare/podzol", "1600"},
                {"mycelium", "buy/rare/mycelium", "2400"}, {"waterlily", "buy/rare/waterlily", "1600"},
                {"dead_bush", "buy/rare/dead_bush", "1200"}
        };
        for (String[] row : rows) {
            int stockGroups = row.length == 4 ? Integer.parseInt(row[3]) : TradeCatalogEntry.UNLIMITED_STOCK;
            String marketKey = LisBamPastoralEconomy.MODID + ":" + row[1];
            MarketCommodity commodity = MarketCatalog.get(marketKey);
            add(definitions, row[0], row[1], TradePool.BUY_UNCOMMON,
                    convertFiniteStock(commodity, stockGroups), Long.parseLong(row[2]));
        }
    }

    private static void addRareBuyDefinitions(Map<String, TradeCatalogEntry> definitions) {
        String[][] rows = {
                {"diamond", "12500", "16", "0.18"},
                {"slime_ball", "2000", "8", "0.18"},
                {"blaze_rod", "9000", "16", "0.18"}, {"ghast_tear", "12000", "16", "0.18"},
                {"ender_pearl", "8000", "16", "0.18"}, {"wither_skeleton_skull", "40000", "4", "0.18"},
                {"shulker_shell", "50000", "8", "0.12"}, {"dragon_breath", "18000", "16", "0.12"},
                {"sponge", "50000", "8", "0.12"}, {"chainmail_helmet", "7500", "8", "0.12"},
                {"chainmail_chestplate", "12500", "8", "0.12"}, {"chainmail_leggings", "11000", "8", "0.12"},
                {"chainmail_boots", "6000", "8", "0.12"}, {"iron_horse_armor", "20000", "8", "0.12"},
                {"golden_horse_armor", "25000", "8", "0.12"}, {"saddle", "18000", "8", "0.12"},
                {"coal_ore", "2134", "8", "0.12"}, {"iron_ore", "2250", "8", "0.12"},
                {"gold_ore", "2500", "8", "0.12"}, {"redstone_ore", "5000", "8", "0.12"},
                {"lapis_ore", "12800", "8", "0.12"}, {"quartz_ore", "2334", "8", "0.12"},
                {"experience_bottle", "1600", "8", "0.12"}, {"name_tag", "10000", "8", "0.12"},
                {"skeleton_skull", "25000", "8", "0.12"}, {"zombie_head", "25000", "8", "0.12"},
                {"creeper_head", "35000", "8", "0.12"}
        };
        for (String[] row : rows) {
            MarketCommodity commodity = MarketCatalog.get(LisBamPastoralEconomy.MODID + ":buy/rare/" + row[0]);
            addChecked(definitions, row[0], "buy/rare/" + row[0], TradePool.BUY_RARE,
                    convertFiniteStock(commodity, Integer.parseInt(row[2])), Long.parseLong(row[1]), Double.parseDouble(row[3]));
        }
    }

    private static void addTreasureDefinitions(Map<String, TradeCatalogEntry> definitions) {
        String[][] rows = {
                {"nether_star", "120000"}, {"enchanted_golden_apple", "250000"}, {"elytra", "1200000"},
                {"dragon_head", "500000"}, {"dragon_egg", "2500000"}, {"diamond_horse_armor", "60000"},
                {"diamond_ore", "16667"}, {"emerald_ore", "7334"}, {"totem_of_undying", "100000"},
                {"record_13", "50000"}, {"record_cat", "50000"}, {"record_blocks", "50000"},
                {"record_chirp", "50000"}, {"record_far", "50000"}, {"record_mall", "50000"},
                {"record_mellohi", "50000"}, {"record_stal", "50000"}, {"record_strad", "50000"},
                {"record_ward", "50000"}, {"record_11", "50000"}, {"record_wait", "50000"},
                {"super_backpack", "200000"}, {"feather_wings", "1200000"}
        };
        for (String[] row : rows) {
            addChecked(definitions, row[0], "buy/treasure/" + row[0], TradePool.BUY_TREASURE,
                    1, Long.parseLong(row[1]), 0.08D);
        }

        addEnchantment(definitions, "mending", Enchantments.MENDING, new int[]{100},
                new long[]{120000}, new String[]{"buy/treasure/enchanted_book/mending_1"});
        addEnchantment(definitions, "frost_walker", Enchantments.FROST_WALKER, new int[]{65, 35},
                new long[]{50000, 80000}, new String[]{"buy/treasure/enchanted_book/frost_walker_1",
                        "buy/treasure/enchanted_book/frost_walker_2"});
        addEnchantment(definitions, "binding_curse", Enchantments.BINDING_CURSE, new int[]{100},
                new long[]{20000}, new String[]{"buy/treasure/enchanted_book/binding_curse_1"});
        addEnchantment(definitions, "vanishing_curse", Enchantments.VANISHING_CURSE, new int[]{100},
                new long[]{20000}, new String[]{"buy/treasure/enchanted_book/vanishing_curse_1"});
        addEnchantment(definitions, "harvest", ModEnchantments.HARVEST, new int[]{50, 35, 15},
                new long[]{50000, 80000, 120000}, new String[]{"buy/treasure/enchanted_book/harvest_1",
                        "buy/treasure/enchanted_book/harvest_2", "buy/treasure/enchanted_book/harvest_3"});
        addEnchantment(definitions, "farmland_walker", ModEnchantments.FARMLAND_WALKER, new int[]{50, 35, 15},
                new long[]{35000, 55000, 80000}, new String[]{"buy/treasure/enchanted_book/tiller_1",
                        "buy/treasure/enchanted_book/tiller_2", "buy/treasure/enchanted_book/tiller_3"});
        addEnchantment(definitions, "pastoral_favor", ModEnchantments.PASTORAL_FAVOR, new int[]{45, 30, 18, 7},
                new long[]{30000, 50000, 80000, 120000}, new String[]{"buy/treasure/enchanted_book/pastoral_favor_1",
                        "buy/treasure/enchanted_book/pastoral_favor_2", "buy/treasure/enchanted_book/pastoral_favor_3",
                        "buy/treasure/enchanted_book/pastoral_favor_4"});
        addEnchantment(definitions, "fine_cultivation", ModEnchantments.FINE_CULTIVATION, new int[]{45, 30, 18, 7},
                new long[]{40000, 60000, 90000, 130000}, new String[]{"buy/treasure/enchanted_book/intensive_farming_1",
                        "buy/treasure/enchanted_book/intensive_farming_2", "buy/treasure/enchanted_book/intensive_farming_3",
                        "buy/treasure/enchanted_book/intensive_farming_4"});
        addEnchantment(definitions, "felling", ModEnchantments.FELLING, new int[]{100},
                new long[]{100000}, new String[]{"buy/treasure/enchanted_book/lumbering_1"});
        addEnchantment(definitions, "slaughter", ModEnchantments.SLAUGHTER, new int[]{50, 35, 15},
                new long[]{60000, 100000, 150000}, new String[]{"buy/treasure/enchanted_book/butchering_1",
                        "buy/treasure/enchanted_book/butchering_2", "buy/treasure/enchanted_book/butchering_3"});
        addEnchantment(definitions, "fleetfoot", ModEnchantments.FLEETFOOT, new int[]{45, 30, 18, 7},
                new long[]{50000, 80000, 120000, 180000}, new String[]{"buy/treasure/enchanted_book/swift_footed_1",
                        "buy/treasure/enchanted_book/swift_footed_2", "buy/treasure/enchanted_book/swift_footed_3",
                        "buy/treasure/enchanted_book/swift_footed_4"});
        addEnchantment(definitions, "night_vision", ModEnchantments.NIGHT_VISION, new int[]{100},
                new long[]{120000}, new String[]{"buy/treasure/enchanted_book/night_vision_1"});
        addEnchantment(definitions, "attack_speed", ModEnchantments.ATTACK_SPEED, new int[]{40, 28, 17, 10, 5},
                new long[]{50000, 80000, 120000, 180000, 250000}, new String[]{"buy/treasure/enchanted_book/attack_speed_1",
                        "buy/treasure/enchanted_book/attack_speed_2", "buy/treasure/enchanted_book/attack_speed_3",
                        "buy/treasure/enchanted_book/attack_speed_4", "buy/treasure/enchanted_book/attack_speed_5"});
        addEnchantment(definitions, "range", ModEnchantments.RANGE, new int[]{40, 28, 17, 10, 5},
                new long[]{60000, 100000, 150000, 210000, 280000}, new String[]{"buy/treasure/enchanted_book/range_1",
                        "buy/treasure/enchanted_book/range_2", "buy/treasure/enchanted_book/range_3",
                        "buy/treasure/enchanted_book/range_4", "buy/treasure/enchanted_book/range_5"});
        addEnchantment(definitions, "reforged", ModEnchantments.REFORGED, new int[]{100},
                new long[]{120000}, new String[]{"buy/treasure/enchanted_book/reforged_1"});
        addEnchantment(definitions, "bluntness_curse", ModEnchantments.BLUNTNESS_CURSE, new int[]{100},
                new long[]{20000}, new String[]{"buy/treasure/enchanted_book/bluntness_curse_1"});
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
                1, false, definition.getCommodity(1), definition));
    }

    private static void addChecked(Map<String, TradeCatalogEntry> definitions, String path, String marketPath,
                                   TradePool pool, int stock, long expectedBasePrice,
                                   double expectedVolatility) {
        add(definitions, path, marketPath, pool, stock, expectedBasePrice);
        TradeCatalogEntry entry = definitions.get(LisBamPastoralEconomy.MODID + ":merchant/" + path);
        if (entry == null || Math.abs(entry.getVolatility() - expectedVolatility) > 0.0000001D) {
            throw new IllegalStateException("Merchant catalog volatility definition is missing or changed: " + path);
        }
    }

    /** Converts the frozen stock policy to actual individual items. */
    private static int convertFiniteStock(MarketCommodity commodity, int legacyGroups) {
        if (legacyGroups == TradeCatalogEntry.UNLIMITED_STOCK) {
            return TradeCatalogEntry.UNLIMITED_STOCK;
        }
        if (legacyGroups <= 0 || commodity == null) {
            throw new IllegalArgumentException("Invalid finite merchant stock definition.");
        }
        // The adjusted price sheet freezes slime balls at 32 individual items;
        // retain the old group notation for all other finite entries.
        if (commodity.getKey().endsWith("buy/rare/slime_ball") && legacyGroups == 8) {
            return 32;
        }
        if (legacyGroups == 1) {
            return 1;
        }
        int groupCount = legacyGroups == 16 ? 4 : legacyGroups == 8 ? 2
                : legacyGroups == 4 ? 1 : legacyGroups;
        long items = (long) groupCount * (long) commodity.getItem().getItemStackLimit();
        if (items <= 0L || items > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Merchant stock overflows item count.");
        }
        return (int) items;
    }

    private static void addRows(Map<String, TradeCatalogEntry> definitions, TradePool pool, int stock, String[][] rows) {
        for (String[] row : rows) {
            add(definitions, row[0], row[1], pool, stock, Long.parseLong(row[2]));
        }
    }

    private static void add(Map<String, TradeCatalogEntry> definitions, String path, String marketPath, TradePool pool,
                            int stock, long expectedBasePrice) {
        String catalogKey = LisBamPastoralEconomy.MODID + ":merchant/" + path;
        String marketKey = LisBamPastoralEconomy.MODID + ":" + marketPath;
        MarketCommodity commodity = MarketCatalog.get(marketKey);
        if (commodity == null) {
            throw new IllegalStateException("Merchant catalog price definition is missing: " + marketKey);
        }
        TradeCatalogEntry previous = definitions.put(catalogKey,
                new TradeCatalogEntry(catalogKey, marketKey, pool, stock, false, commodity));
        if (previous != null) {
            throw new IllegalStateException("Duplicate merchant catalog key: " + catalogKey);
        }
    }

    private static void addAnyWool(Map<String, TradeCatalogEntry> definitions, String path, String marketPath,
                                   TradePool pool, long expectedBasePrice) {
        String catalogKey = LisBamPastoralEconomy.MODID + ":merchant/" + path;
        String marketKey = LisBamPastoralEconomy.MODID + ":" + marketPath;
        MarketCommodity commodity = MarketCatalog.get(marketKey);
        if (commodity == null) {
            throw new IllegalStateException("Merchant wool price definition is missing.");
        }
        definitions.put(catalogKey, new TradeCatalogEntry(catalogKey, marketKey, pool,
                TradeCatalogEntry.UNLIMITED_STOCK, true, commodity));
    }
}
