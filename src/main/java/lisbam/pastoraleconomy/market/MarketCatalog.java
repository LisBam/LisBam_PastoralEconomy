package lisbam.pastoraleconomy.market;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The single frozen market directory. Keys name an economic price definition,
 * so a unit sell price and a later merchant purchase bundle can coexist for
 * the same Minecraft Item without confusing either transaction direction.
 */
public final class MarketCatalog {
    private static final Map<String, MarketCommodity> BY_KEY;
    private static final List<MarketCommodity> ALL;
    private static final List<MarketCommodity> HISTORY_TRACKED;

    static {
        Map<String, MarketCommodity> definitions = new LinkedHashMap<String, MarketCommodity>();
        addSellingDefinitions(definitions);
        addCommonAndRarePurchaseDefinitions(definitions);
        addRareAndTreasurePurchaseDefinitions(definitions);
        validate(definitions);
        BY_KEY = Collections.unmodifiableMap(definitions);
        ALL = Collections.unmodifiableList(new ArrayList<MarketCommodity>(definitions.values()));
        List<MarketCommodity> tracked = new ArrayList<MarketCommodity>();
        for (MarketCommodity commodity : ALL) {
            if (commodity.isHistoryTracked()) {
                tracked.add(commodity);
            }
        }
        if (tracked.size() != 14) {
            throw new IllegalStateException("The market must track exactly fourteen crop histories.");
        }
        HISTORY_TRACKED = Collections.unmodifiableList(tracked);
    }

    private MarketCatalog() {
    }

    public static MarketCommodity get(String key) {
        return BY_KEY.get(key);
    }

    public static List<MarketCommodity> getAll() {
        return ALL;
    }

    public static List<MarketCommodity> getHistoryTracked() {
        return HISTORY_TRACKED;
    }

    private static void addSellingDefinitions(Map<String, MarketCommodity> definitions) {
        add(definitions, "sell/crop/wheat", Items.WHEAT, 0, 5, CommodityCategory.CORE_CROPS, true);
        add(definitions, "sell/crop/carrot", Items.CARROT, 0, 5, CommodityCategory.CORE_CROPS, true);
        add(definitions, "sell/crop/potato", Items.POTATO, 0, 4, CommodityCategory.CORE_CROPS, true);
        add(definitions, "sell/crop/beetroot", Items.BEETROOT, 0, 7, CommodityCategory.CORE_CROPS, true);
        add(definitions, "sell/crop/pumpkin", block(Blocks.PUMPKIN), 0, 17, CommodityCategory.CORE_CROPS, true);
        add(definitions, "sell/crop/melon_block", block(Blocks.MELON_BLOCK), 0, 25, CommodityCategory.CORE_CROPS, true);
        add(definitions, "sell/crop/melon_slice", Items.MELON, 0, 3, CommodityCategory.CORE_CROPS, true);
        add(definitions, "sell/crop/sugar_cane", Items.REEDS, 0, 4, CommodityCategory.CORE_CROPS, true);
        add(definitions, "sell/crop/cactus", block(Blocks.CACTUS), 0, 3, CommodityCategory.CORE_CROPS, true);
        add(definitions, "sell/crop/cocoa_beans", Items.DYE, 3, 5, CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, true);
        add(definitions, "sell/crop/red_mushroom", block(Blocks.RED_MUSHROOM), 0, 5, CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, true);
        add(definitions, "sell/crop/brown_mushroom", block(Blocks.BROWN_MUSHROOM), 0, 5, CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, true);
        add(definitions, "sell/agriculture/apple", Items.APPLE, 0, 10, CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, false);
        add(definitions, "sell/livestock/egg", Items.EGG, 0, 6, CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, false);
        add(definitions, "sell/livestock/feather", Items.FEATHER, 0, 5, CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, false);
        add(definitions, "sell/livestock/leather", Items.LEATHER, 0, 12, CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, false);
        add(definitions, "sell/livestock/rabbit_hide", Items.RABBIT_HIDE, 0, 6, CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, false);
        add(definitions, "sell/livestock/rabbit_foot", Items.RABBIT_FOOT, 0, 20, CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, false);
        add(definitions, "sell/crop/nether_wart", Items.NETHER_WART, 0, 8, CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, true);
        add(definitions, "sell/crop/chorus_fruit", Items.CHORUS_FRUIT, 0, 8, CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, true);
        add(definitions, "sell/livestock/milk_bucket", Items.MILK_BUCKET, 0, 20, CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, false);
        // Merchant offers treat all 16 wool metadata values as one logical good,
        // so it needs one shared frozen unit price rather than sixteen rolls.
        add(definitions, "sell/livestock/wool", block(Blocks.WOOL), 0, 8,
                CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, false, "minecraft:wool@any_color");
        for (int metadata = 0; metadata < 16; metadata++) {
            add(definitions, "sell/livestock/wool_" + metadata, block(Blocks.WOOL), metadata, 8,
                    CommodityCategory.SECONDARY_AGRICULTURE_LIVESTOCK, false);
        }
    }

    private static void addCommonAndRarePurchaseDefinitions(Map<String, MarketCommodity> definitions) {
        add(definitions, "buy/common/wheat_seeds", Items.WHEAT_SEEDS, 0, 80, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/common/carrot", Items.CARROT, 0, 80, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/common/potato", Items.POTATO, 0, 80, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/common/beetroot_seeds", Items.BEETROOT_SEEDS, 0, 96, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/common/pumpkin_seeds", Items.PUMPKIN_SEEDS, 0, 120, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/common/melon_seeds", Items.MELON_SEEDS, 0, 120, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/common/cocoa_beans", Items.DYE, 3, 120, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/common/sugar_cane", Items.REEDS, 0, 80, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/common/cactus", block(Blocks.CACTUS), 0, 80, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/common/red_mushroom", block(Blocks.RED_MUSHROOM), 0, 120, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/common/brown_mushroom", block(Blocks.BROWN_MUSHROOM), 0, 120, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/common/bone_meal", Items.DYE, 15, 180, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/common/nether_wart", Items.NETHER_WART, 0, 400, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/common/chorus_flower", block(Blocks.CHORUS_FLOWER), 0, 500, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        addSaplings(definitions);
        addLogsAndBuildingMaterials(definitions);
        addCommonResources(definitions);
        addRareResources(definitions);
    }

    private static void addSaplings(Map<String, MarketCommodity> definitions) {
        String[] names = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
        for (int metadata = 0; metadata < names.length; metadata++) {
            add(definitions, "buy/common/sapling_" + names[metadata], block(Blocks.SAPLING), metadata, 120,
                    CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        }
    }

    private static void addLogsAndBuildingMaterials(Map<String, MarketCommodity> definitions) {
        String[] names = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
        for (int metadata = 0; metadata < names.length; metadata++) {
            Block log = metadata < 4 ? Blocks.LOG : Blocks.LOG2;
            int logMetadata = metadata < 4 ? metadata : metadata - 4;
            add(definitions, "buy/common/log_" + names[metadata], block(log), logMetadata, 100,
                    CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        }
        add(definitions, "buy/common/dirt", block(Blocks.DIRT), 0, 32, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/cobblestone", block(Blocks.COBBLESTONE), 0, 48, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/stone", block(Blocks.STONE), 0, 64, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/granite", block(Blocks.STONE), 1, 96, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/andesite", block(Blocks.STONE), 5, 96, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/diorite", block(Blocks.STONE), 3, 96, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/sand", block(Blocks.SAND), 0, 96, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/red_sand", block(Blocks.SAND), 1, 128, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/gravel", block(Blocks.GRAVEL), 0, 80, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/clay_ball", Items.CLAY_BALL, 0, 96, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/glass", block(Blocks.GLASS), 0, 160, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/ice", block(Blocks.ICE), 0, 80, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/packed_ice", block(Blocks.PACKED_ICE), 0, 320, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/snow", block(Blocks.SNOW), 0, 64, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
        add(definitions, "buy/common/obsidian", block(Blocks.OBSIDIAN), 0, 400, CommodityCategory.BASIC_BUILDING_MATERIALS, false);
    }

    private static void addCommonResources(Map<String, MarketCommodity> definitions) {
        Object[][] resources = {
                {"coal", Items.COAL, 0, 160}, {"charcoal", Items.COAL, 1, 160},
                {"iron_ingot", Items.IRON_INGOT, 0, 360}, {"redstone", Items.REDSTONE, 0, 240},
                {"lapis_lazuli", Items.DYE, 4, 280}, {"flint", Items.FLINT, 0, 160},
                {"bone", Items.BONE, 0, 240}, {"string", Items.STRING, 0, 240},
                {"spider_eye", Items.SPIDER_EYE, 0, 160}, {"rotten_flesh", Items.ROTTEN_FLESH, 0, 160},
                {"ink_sac", Items.DYE, 0, 160}, {"netherrack", block(Blocks.NETHERRACK), 0, 96},
                {"soul_sand", block(Blocks.SOUL_SAND), 0, 200}, {"egg", Items.EGG, 0, 80},
                {"feather", Items.FEATHER, 0, 120}
        };
        addRows(definitions, "buy/common/", resources, CommodityCategory.MINERALS_REDSTONE_COMMON_DROPS);
    }

    private static void addRareResources(Map<String, MarketCommodity> definitions) {
        Object[][] resources = {
                {"gold_ingot", Items.GOLD_INGOT, 0, 800}, {"lava_bucket", Items.LAVA_BUCKET, 0, 600},
                {"gunpowder", Items.GUNPOWDER, 0, 400}, {"slime_ball", Items.SLIME_BALL, 0, 480},
                {"magma_cream", Items.MAGMA_CREAM, 0, 640}, {"glowstone_dust", Items.GLOWSTONE_DUST, 0, 480},
                {"nether_quartz", Items.QUARTZ, 0, 480}, {"end_stone", block(Blocks.END_STONE), 0, 320},
                {"prismarine_shard", Items.PRISMARINE_SHARD, 0, 400}, {"prismarine_crystals", Items.PRISMARINE_CRYSTALS, 0, 400},
                {"leather", Items.LEATHER, 0, 200}, {"rabbit_hide", Items.RABBIT_HIDE, 0, 160},
                {"rabbit_foot", Items.RABBIT_FOOT, 0, 400}, {"milk_bucket", Items.MILK_BUCKET, 0, 180},
                {"beef", Items.BEEF, 0, 200}, {"porkchop", Items.PORKCHOP, 0, 200},
                {"chicken", Items.CHICKEN, 0, 160}, {"mutton", Items.MUTTON, 0, 200},
                {"rabbit", Items.RABBIT, 0, 240}, {"fish", Items.FISH, 0, 160},
                {"salmon", Items.FISH, 1, 200}, {"clownfish", Items.FISH, 2, 240}, {"pufferfish", Items.FISH, 3, 240}
        };
        addRows(definitions, "buy/rare/", resources, CommodityCategory.MINERALS_REDSTONE_COMMON_DROPS);
        add(definitions, "buy/rare/cobweb", block(Blocks.WEB), 0, 300, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        String[] flowerNames = {"dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy"};
        for (int metadata = 0; metadata < flowerNames.length; metadata++) {
            Block flower = metadata == 0 ? Blocks.YELLOW_FLOWER : Blocks.RED_FLOWER;
            int flowerMeta = metadata == 0 ? 0 : metadata - 1;
            int price = metadata == 0 || metadata == 1 ? 120 : 160;
            add(definitions, "buy/rare/" + flowerNames[metadata], block(flower), flowerMeta, price,
                    CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        }
        String[] tallFlowerNames = {"sunflower", "lilac", "rose_bush", "peony"};
        int[] tallFlowerMetadata = {0, 1, 4, 5};
        for (int index = 0; index < tallFlowerNames.length; index++) {
            add(definitions, "buy/rare/" + tallFlowerNames[index], block(Blocks.DOUBLE_PLANT), tallFlowerMetadata[index], 160,
                    CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        }
        add(definitions, "buy/rare/vine", block(Blocks.VINE), 0, 160, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/rare/podzol", block(Blocks.DIRT), 2, 160, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/rare/mycelium", block(Blocks.MYCELIUM), 0, 240, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/rare/waterlily", block(Blocks.WATERLILY), 0, 160, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
        add(definitions, "buy/rare/dead_bush", block(Blocks.DEADBUSH), 0, 120, CommodityCategory.SEEDS_AND_AGRICULTURAL_SUPPLIES, false);
    }

    private static void addRareAndTreasurePurchaseDefinitions(Map<String, MarketCommodity> definitions) {
        Object[][] advanced = {
                {"diamond", Items.DIAMOND, 0, 1600}, {"emerald", Items.EMERALD, 0, 700},
                {"blaze_rod", Items.BLAZE_ROD, 0, 900}, {"ghast_tear", Items.GHAST_TEAR, 0, 1200},
                {"ender_pearl", Items.ENDER_PEARL, 0, 800}, {"wither_skeleton_skull", Items.SKULL, 1, 4000}
        };
        addRows(definitions, "buy/rare/", advanced, CommodityCategory.ADVANCED_NETHER_END_RESOURCES);
        Object[][] rare = {
                {"shulker_shell", Items.SHULKER_SHELL, 0, 5000}, {"dragon_breath", Items.DRAGON_BREATH, 0, 1800},
                {"sponge", block(Blocks.SPONGE), 0, 5000}, {"chainmail_helmet", Items.CHAINMAIL_HELMET, 0, 1500},
                {"chainmail_chestplate", Items.CHAINMAIL_CHESTPLATE, 0, 2500}, {"chainmail_leggings", Items.CHAINMAIL_LEGGINGS, 0, 2200},
                {"chainmail_boots", Items.CHAINMAIL_BOOTS, 0, 1200}, {"iron_horse_armor", Items.IRON_HORSE_ARMOR, 0, 2000},
                {"golden_horse_armor", Items.GOLDEN_HORSE_ARMOR, 0, 2500}, {"saddle", Items.SADDLE, 0, 1800},
                {"coal_ore", block(Blocks.COAL_ORE), 0, 400}, {"iron_ore", block(Blocks.IRON_ORE), 0, 500},
                {"gold_ore", block(Blocks.GOLD_ORE), 0, 1000}, {"redstone_ore", block(Blocks.REDSTONE_ORE), 0, 800},
                {"lapis_ore", block(Blocks.LAPIS_ORE), 0, 900}, {"quartz_ore", block(Blocks.QUARTZ_ORE), 0, 700},
                {"experience_bottle", Items.EXPERIENCE_BOTTLE, 0, 1200}, {"name_tag", Items.NAME_TAG, 0, 1000},
                {"skeleton_skull", Items.SKULL, 0, 2500}, {"zombie_head", Items.SKULL, 2, 2500}, {"creeper_head", Items.SKULL, 4, 3500}
        };
        addRows(definitions, "buy/rare/", rare, CommodityCategory.RARE_GOODS);
        Object[][] treasures = {
                {"nether_star", Items.NETHER_STAR, 0, 12000}, {"enchanted_golden_apple", Items.GOLDEN_APPLE, 1, 25000},
                {"elytra", Items.ELYTRA, 0, 120000}, {"dragon_head", Items.SKULL, 5, 10000},
                {"dragon_egg", block(Blocks.DRAGON_EGG), 0, 250000}, {"diamond_horse_armor", Items.DIAMOND_HORSE_ARMOR, 0, 6000},
                {"diamond_ore", block(Blocks.DIAMOND_ORE), 0, 3000}, {"emerald_ore", block(Blocks.EMERALD_ORE), 0, 2500},
                {"totem_of_undying", Items.TOTEM_OF_UNDYING, 0, 10000}, {"record_13", Items.RECORD_13, 0, 5000},
                {"record_cat", Items.RECORD_CAT, 0, 5000}, {"record_blocks", Items.RECORD_BLOCKS, 0, 5000},
                {"record_chirp", Items.RECORD_CHIRP, 0, 5000}, {"record_far", Items.RECORD_FAR, 0, 5000},
                {"record_mall", Items.RECORD_MALL, 0, 5000}, {"record_mellohi", Items.RECORD_MELLOHI, 0, 5000},
                {"record_stal", Items.RECORD_STAL, 0, 5000}, {"record_strad", Items.RECORD_STRAD, 0, 5000},
                {"record_ward", Items.RECORD_WARD, 0, 5000}, {"record_11", Items.RECORD_11, 0, 5000}, {"record_wait", Items.RECORD_WAIT, 0, 5000}
        };
        addRows(definitions, "buy/treasure/", treasures, CommodityCategory.TREASURES_COLLECTIBLES);
        addEnchantedBookDefinitions(definitions);
    }

    private static void addEnchantedBookDefinitions(Map<String, MarketCommodity> definitions) {
        book(definitions, "mending_1", 12000); book(definitions, "frost_walker_1", 5000); book(definitions, "frost_walker_2", 8000);
        book(definitions, "binding_curse_1", 2000); book(definitions, "vanishing_curse_1", 2000);
        book(definitions, "harvest_1", 5000); book(definitions, "harvest_2", 8000); book(definitions, "harvest_3", 12000);
        book(definitions, "tiller_1", 3500); book(definitions, "tiller_2", 5500); book(definitions, "tiller_3", 8000);
        book(definitions, "pastoral_favor_1", 3000); book(definitions, "pastoral_favor_2", 5000); book(definitions, "pastoral_favor_3", 8000); book(definitions, "pastoral_favor_4", 12000);
        book(definitions, "intensive_farming_1", 4000); book(definitions, "intensive_farming_2", 6000); book(definitions, "intensive_farming_3", 9000); book(definitions, "intensive_farming_4", 13000);
        book(definitions, "lumbering_1", 10000);
        book(definitions, "butchering_1", 6000); book(definitions, "butchering_2", 10000); book(definitions, "butchering_3", 15000);
        book(definitions, "swift_footed_1", 5000); book(definitions, "swift_footed_2", 8000); book(definitions, "swift_footed_3", 12000); book(definitions, "swift_footed_4", 18000);
        book(definitions, "night_vision_1", 12000);
    }

    private static void book(Map<String, MarketCommodity> definitions, String name, long price) {
        add(definitions, "buy/treasure/enchanted_book/" + name, Items.ENCHANTED_BOOK, 0, price,
                CommodityCategory.TREASURES_COLLECTIBLES, false, "enchantment/" + name);
    }

    private static void addRows(Map<String, MarketCommodity> definitions, String prefix, Object[][] rows, CommodityCategory category) {
        for (Object[] row : rows) {
            add(definitions, prefix + (String) row[0], (Item) row[1], (Integer) row[2], (Integer) row[3], category, false);
        }
    }

    private static Item block(Block block) {
        Item item = Item.getItemFromBlock(block);
        if (item == Items.AIR) {
            throw new IllegalStateException("No 1.12.2 Item mapping for block " + block.getRegistryName());
        }
        return item;
    }

    private static void add(Map<String, MarketCommodity> definitions, String path, Item item, int metadata, long basePrice,
                            CommodityCategory category, boolean historyTracked) {
        add(definitions, path, item, metadata, basePrice, category, historyTracked,
                item.getRegistryName().toString() + "@" + metadata);
    }

    private static void add(Map<String, MarketCommodity> definitions, String path, Item item, int metadata, long basePrice,
                            CommodityCategory category, boolean historyTracked, String variantIdentity) {
        String key = LisBamPastoralEconomy.MODID + ":" + path;
        MarketCommodity previous = definitions.put(key, new MarketCommodity(key, item, metadata, basePrice, category,
                historyTracked, variantIdentity));
        if (previous != null) {
            throw new IllegalStateException("Duplicate market commodity key: " + key);
        }
    }

    private static void validate(Map<String, MarketCommodity> definitions) {
        if (definitions.isEmpty()) {
            throw new IllegalStateException("Market catalog must not be empty.");
        }
        for (MarketCommodity commodity : definitions.values()) {
            if (commodity.getItem().getRegistryName() == null) {
                throw new IllegalStateException("Market item has no registry identity: " + commodity.getKey());
            }
        }
    }
}
