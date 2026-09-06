package lisbam.pastoraleconomy.item;

import lisbam.pastoraleconomy.block.ModBlocks;
import net.minecraft.item.ItemBlock;

/** Stable item allocation point; registration remains in RegistrationHandler. */
public final class ModItems {
    public static final PastoralCreativeTab CREATIVE_TAB = PastoralCreativeTab.INSTANCE;
    public static final ItemBackpack BACKPACK = new ItemBackpack("backpack", 27);
    public static final ItemBackpack ADVANCED_BACKPACK = new ItemBackpack("advanced_backpack", 27 * 2);
    public static final ItemBackpack SUPER_BACKPACK = new ItemBackpack("super_backpack", 27 * 4);
    public static final ItemMarketBook MARKET_BOOK = new ItemMarketBook();
    public static final ItemTradeVoucher TRADE_VOUCHER = new ItemTradeVoucher();
    public static final ItemGoldenBoneMeal GOLDEN_BONE_MEAL = new ItemGoldenBoneMeal();
    public static final ItemMerchantSpawnEgg MERCHANT_SPAWN_EGG = new ItemMerchantSpawnEgg();
    public static final ItemBlock CRAB_TRAP_ITEM = createCrabTrapItem();
    public static final ItemBlock TRANSPORT_STATION_ITEM = createTransportStationItem();

    private ModItems() {
    }

    private static ItemBlock createCrabTrapItem() {
        ItemBlock item = new ItemBlock(ModBlocks.CRAB_TRAP);
        item.setRegistryName(ModBlocks.CRAB_TRAP.getRegistryName());
        return item;
    }

    private static ItemBlock createTransportStationItem() {
        return new ItemTransportStation();
    }
}
