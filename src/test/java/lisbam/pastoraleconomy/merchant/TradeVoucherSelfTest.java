package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.item.ItemTradeVoucher;
import lisbam.pastoraleconomy.item.ModItems;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.UUID;

/** Standalone checks for immutable voucher binding and rollback-safe composite sale stock. */
public final class TradeVoucherSelfTest {
    private TradeVoucherSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        Bootstrap.register();
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        ItemStack voucher = new ItemStack(ModItems.TRADE_VOUCHER);
        require(!ItemTradeVoucher.isBound(voucher), "new voucher must be blank");
        require(ItemTradeVoucher.bind(voucher, owner, "TestPlayer"), "blank voucher binds once");
        require(ItemTradeVoucher.isBoundTo(voucher, owner), "bound UUID is authoritative");
        require(!ItemTradeVoucher.isBoundTo(voucher, stranger), "different player cannot use voucher");
        require("TestPlayer".equals(ItemTradeVoucher.getBoundPlayerName(voucher)), "display owner persists");
        require(!ItemTradeVoucher.bind(voucher, stranger, "Other"), "bound voucher cannot be rebound");

        ItemStack restoredVoucher = new ItemStack(ModItems.TRADE_VOUCHER);
        restoredVoucher.setTagCompound(voucher.getTagCompound().copy());
        require(ItemTradeVoucher.isBoundTo(restoredVoucher, owner), "voucher UUID survives ItemStack NBT copy");
        require("TestPlayer".equals(ItemTradeVoucher.getBoundPlayerName(restoredVoucher)),
                "voucher name survives ItemStack NBT copy");

        InventoryBasic player = new InventoryBasic("player", false, 36);
        InventoryBasic chest = new InventoryBasic("chest", false, 27);
        player.setInventorySlotContents(0, new ItemStack(Items.WHEAT, 3));
        chest.setInventorySlotContents(0, restoredVoucher);
        chest.setInventorySlotContents(1, new ItemStack(Items.WHEAT, 7));
        require(TradeVoucherStorageService.containsVoucher(chest, owner), "owner voucher authorizes chest");
        require(!TradeVoucherStorageService.containsVoucher(chest, stranger), "voucher does not authorize stranger");

        TradeVoucherStorageService.SaleInventory stock = TradeVoucherStorageService.createForInventories(
                player, 36, Arrays.<IInventory>asList(chest));
        TradeCatalogEntry wheat = TradeCatalog.get("lisbam_pastoral_economy:merchant/wheat");
        require(wheat != null && stock.count(wheat) == 10, "player and voucher chest stock combine");
        require(stock.countLinkedChests(wheat) == 7, "snapshot exposes linked chest stock separately");
        TradeVoucherStorageService.Snapshot before = stock.snapshot();
        require(stock.remove(wheat, 8) == 8 && stock.count(wheat) == 2,
                "sale removes player stock before linked chest stock");
        require(ItemTradeVoucher.isBoundTo(chest.getStackInSlot(0), owner), "sale never consumes voucher");
        stock.restore(before);
        require(stock.count(wheat) == 10, "failed transaction snapshot restores every source");

        player.clear();
        chest.clear();
        chest.setInventorySlotContents(0, restoredVoucher);
        require(stock.insert(new ItemStack(Items.BUCKET, 20)), "returned buckets span player and chest slots");
        require(countItem(player, Items.BUCKET) + countItem(chest, Items.BUCKET) == 20,
                "returned bucket count is exact");

        verifyVoucherChestRegistry();

        verifyTexture("trade_voucher_empty.png");
        verifyTexture("trade_voucher_bound.png");
        System.out.println("tradeVoucherSelfTest PASS");
    }

    private static void verifyVoucherChestRegistry() {
        VoucherChestRegistry registry = new VoucherChestRegistry();
        BlockPos overworldChest = new BlockPos(32, 70, -48);
        BlockPos netherChest = new BlockPos(-17, 64, 49);
        require(registry.add(0, overworldChest) && registry.add(-1, netherChest),
                "bound voucher chest positions enter the world registry");
        require(!registry.add(0, overworldChest), "the same physical chest is not indexed twice");

        NBTTagCompound saved = registry.writeToNBT();
        VoucherChestRegistry restored = new VoucherChestRegistry();
        restored.readFromNBT(saved);
        require(restored.getLocations().size() == 2, "voucher chest registry survives WorldSavedData NBT");
        boolean foundOverworld = false;
        boolean foundNether = false;
        for (VoucherChestRegistry.Location location : restored.getLocations()) {
            foundOverworld |= location.getDimension() == 0 && overworldChest.equals(location.getPosition());
            foundNether |= location.getDimension() == -1 && netherChest.equals(location.getPosition());
        }
        require(foundOverworld && foundNether,
                "voucher chest registry keeps exact dimension and block position for ticket recovery");
    }

    private static int countItem(IInventory inventory, net.minecraft.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void verifyTexture(String name) throws Exception {
        File file = new File("src/main/resources/assets/lisbam_pastoral_economy/textures/items", name);
        BufferedImage image = ImageIO.read(file);
        require(image != null && image.getWidth() == 16 && image.getHeight() == 16,
                name + " must be a readable 16x16 PNG");
        require(image.getColorModel().hasAlpha() && (image.getRGB(0, 0) >>> 24) == 0,
                name + " must have transparent item padding");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
