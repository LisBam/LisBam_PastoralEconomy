package lisbam.pastoraleconomy.shipping;

import lisbam.pastoraleconomy.item.ItemTradeVoucher;
import lisbam.pastoraleconomy.item.ModItems;
import lisbam.pastoraleconomy.market.MarketPriceSnapshot;
import lisbam.pastoraleconomy.merchant.TradeCatalog;
import lisbam.pastoraleconomy.merchant.TradeCatalogEntry;
import lisbam.pastoraleconomy.tile.TileShippingBox;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Standalone regression checks for chest capacity, hopper policy, and daily settlement invariants. */
public final class ShippingBoxSelfTest {
    private ShippingBoxSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        Bootstrap.register();
        GameRegistry.registerTileEntity(TileShippingBox.class,
                new ResourceLocation("minecraft", "shipping_box_self_test"));
        verifyInventoryAndNbt();
        verifySettlementPlan();
        verifyPayoutPersistence();
        verifyTexture();
        System.out.println("shippingBoxSelfTest PASS");
    }

    private static void verifyInventoryAndNbt() {
        TileShippingBox box = new TileShippingBox();
        require(box.getSizeInventory() == 27, "shipping box must match a single chest's 27 slots");
        require(box.getSlotsForFace(net.minecraft.util.EnumFacing.UP).length == 27,
                "every slot must accept hopper input from each side");
        require(box.canInsertItem(0, new ItemStack(Items.WHEAT), net.minecraft.util.EnumFacing.DOWN),
                "hoppers may insert ordinary stacks");
        require(!box.canExtractItem(0, new ItemStack(Items.WHEAT), net.minecraft.util.EnumFacing.DOWN),
                "hoppers must never extract from the shipping box");
        box.setPos(BlockPos.ORIGIN);
        box.setInventorySlotContents(26, new ItemStack(Items.WHEAT, 12));
        NBTTagCompound saved = box.writeToNBT(new NBTTagCompound());
        TileShippingBox restored = new TileShippingBox();
        restored.readFromNBT(saved);
        require(restored.getStackInSlot(26).getCount() == 12,
                "all 27 slots must survive tile NBT round-trip");
    }

    private static void verifySettlementPlan() {
        UUID firstOwner = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID secondOwner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        ItemStack firstVoucher = new ItemStack(ModItems.TRADE_VOUCHER);
        ItemStack duplicateFirstVoucher = new ItemStack(ModItems.TRADE_VOUCHER);
        ItemStack secondVoucher = new ItemStack(ModItems.TRADE_VOUCHER);
        require(ItemTradeVoucher.bind(firstVoucher, firstOwner, "First"), "first voucher binds");
        require(ItemTradeVoucher.bind(duplicateFirstVoucher, firstOwner, "First"), "duplicate owner voucher binds");
        require(ItemTradeVoucher.bind(secondVoucher, secondOwner, "Second"), "second voucher binds");

        TileShippingBox box = new TileShippingBox();
        box.setInventorySlotContents(0, firstVoucher);
        box.setInventorySlotContents(1, duplicateFirstVoucher);
        box.setInventorySlotContents(2, secondVoucher);
        box.setInventorySlotContents(3, new ItemStack(Items.WHEAT, 10));
        box.setInventorySlotContents(4, new ItemStack(Items.DIAMOND, 2));
        TradeCatalogEntry wheat = TradeCatalog.findSellEntry(box.getStackInSlot(3));
        require(wheat != null && TradeCatalog.findSellEntry(box.getStackInSlot(4)) == null,
                "only existing merchant purchase goods are eligible for shipping");

        Map<String, Long> prices = new LinkedHashMap<String, Long>();
        prices.put(wheat.getMarketKey(), Long.valueOf(50L));
        ShippingBoxService.SalePlan plan = ShippingBoxService.createSalePlan(box,
                new MarketPriceSnapshot(12L, prices, new LinkedHashMap<String, Long>()));
        require(plan != null && plan.getSoldStackCount() == 1, "sale plan must retain only eligible goods");
        require(sum(plan.getAllocations()) == 350L, "10 wheat at 50 must pay exactly floor(500 * 70%)");
        require(plan.getAllocations().size() == 2, "duplicate vouchers of one player must not duplicate shares");
        require(plan.getAllocations().get(secondOwner).longValue() == 175L
                        && plan.getAllocations().get(firstOwner).longValue() == 175L,
                "two distinct voucher owners split a box's payout evenly");

        require(ShippingBoxRules.calculateDiscountedIncome(999L) == 699L,
                "70% settlement must floor fractional coins");
        require(ShippingBoxRules.calculateGrossIncome(Long.MAX_VALUE, 2) < 0L,
                "overflowing gross income must be rejected before inventory mutation");
        require(sum(ShippingBoxRules.splitEvenly(5L, Arrays.asList(firstOwner, secondOwner))) == 5L,
                "remainder distribution must not lose coins");
        require(ShippingBoxRules.isMorningSettlementWindow(0L)
                        && ShippingBoxRules.isMorningSettlementWindow(1000L)
                        && ShippingBoxRules.isMorningSettlementWindow(24000L),
                "the day-start and vanilla day-command morning window must dispatch");
        require(!ShippingBoxRules.isMorningSettlementWindow(1001L)
                        && !ShippingBoxRules.isMorningSettlementWindow(23999L),
                "late-day ticks must not consume the next morning's dispatch marker");
    }

    private static void verifyPayoutPersistence() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000010");
        ShippingBoxPayoutState state = new ShippingBoxPayoutState();
        require(state.tryBeginDispatch(7L) && !state.tryBeginDispatch(7L) && !state.tryBeginDispatch(6L),
                "each stored world day may dispatch only once, including after a time rollback");
        Map<UUID, Long> allocation = new LinkedHashMap<UUID, Long>();
        allocation.put(owner, Long.valueOf(700L));
        require(state.canCredit(allocation) && state.credit(allocation), "valid deferred owner income is accepted");
        ShippingBoxPayoutState restored = new ShippingBoxPayoutState();
        restored.readFromNBT(state.writeToNBT());
        require(restored.getLastDispatchDay() == 7L && restored.getPending(owner) == 700L,
                "dispatch marker and offline income must survive WorldSavedData NBT");
        require(restored.claim(owner, 700L) && restored.getPending(owner) == 0L,
                "a credited payment is removed exactly once");
    }

    private static long sum(Map<UUID, Long> values) {
        long result = 0L;
        for (Long value : values.values()) {
            result += value.longValue();
        }
        return result;
    }

    private static void verifyTexture() throws Exception {
        File file = new File("src/main/resources/assets/lisbam_pastoral_economy/textures/blocks/shipping_box.png");
        BufferedImage image = ImageIO.read(file);
        require(image != null && image.getWidth() == 16 && image.getHeight() == 16,
                "shipping-box texture must be a readable 16x16 PNG");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
