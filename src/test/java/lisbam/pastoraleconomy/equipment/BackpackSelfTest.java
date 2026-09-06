package lisbam.pastoraleconomy.equipment;

import lisbam.pastoraleconomy.item.ModItems;
import lisbam.pastoraleconomy.item.RecipeAdvancedBackpack;
import lisbam.pastoraleconomy.network.message.RequestOpenBackpackMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** Deterministic capacity, NBT and crafting data-loss regression checks. */
public final class BackpackSelfTest {
    private BackpackSelfTest() {
    }

    public static void main(String[] args) {
        Bootstrap.register();
        check(ModItems.BACKPACK.getCapacity() == 27 && ModItems.BACKPACK.getPageCount() == 1,
                "ordinary backpack capacity");
        check(ModItems.ADVANCED_BACKPACK.getCapacity() == 54 && ModItems.ADVANCED_BACKPACK.getPageCount() == 2,
                "advanced backpack capacity");
        check(ModItems.SUPER_BACKPACK.getCapacity() == 108 && ModItems.SUPER_BACKPACK.getPageCount() == 4,
                "super backpack capacity");
        check(ModItems.BACKPACK.getItemStackLimit() == 1
                        && ModItems.ADVANCED_BACKPACK.getItemStackLimit() == 1
                        && ModItems.SUPER_BACKPACK.getItemStackLimit() == 1,
                "all backpack tiers are non-stackable");

        ItemStack superBackpack = new ItemStack(ModItems.SUPER_BACKPACK);
        NonNullList<ItemStack> contents = BackpackStorage.read(superBackpack);
        contents.set(0, new ItemStack(Items.APPLE, 17));
        contents.set(53, new ItemStack(Items.DIAMOND, 3));
        contents.set(107, new ItemStack(Items.LEATHER, 64));
        BackpackStorage.write(superBackpack, contents);
        NonNullList<ItemStack> restored = BackpackStorage.read(superBackpack);
        check(restored.size() == 108 && restored.get(0).getCount() == 17
                        && restored.get(53).getCount() == 3 && restored.get(107).getCount() == 64,
                "all four storage pages round-trip");

        restored.set(1, new ItemStack(ModItems.BACKPACK));
        BackpackStorage.write(superBackpack, restored);
        check(BackpackStorage.read(superBackpack).get(1).isEmpty(), "nested backpacks are never serialized");

        RecipeAdvancedBackpack recipe = new RecipeAdvancedBackpack();
        InventoryCrafting crafting = createAdvancedRecipeGrid();
        check(recipe.matches(crafting, null), "two empty ordinary backpacks match the advanced recipe");
        NonNullList<ItemStack> usedContents = BackpackStorage.read(crafting.getStackInSlot(1));
        usedContents.set(0, new ItemStack(Items.APPLE));
        BackpackStorage.write(crafting.getStackInSlot(1), usedContents);
        check(!recipe.matches(crafting, null), "a non-empty backpack cannot be consumed by crafting");
        verifyOpenRequestBounds();
        verifyTexture("backpack.png");
        verifyTexture("advanced_backpack.png");
        verifyTexture("super_backpack.png");
        System.out.println("backpackSelfTest PASS");
    }

    private static void verifyOpenRequestBounds() {
        ByteBuf empty = Unpooled.buffer();
        ByteBuf trailing = Unpooled.buffer();
        try {
            RequestOpenBackpackMessage valid = new RequestOpenBackpackMessage();
            valid.fromBytes(empty);
            check(valid.isValid(), "empty open-backpack request is valid");
            trailing.writeByte(1);
            RequestOpenBackpackMessage invalid = new RequestOpenBackpackMessage();
            invalid.fromBytes(trailing);
            check(!invalid.isValid(), "open-backpack request rejects trailing payload");
        } finally {
            empty.release();
            trailing.release();
        }
    }

    private static InventoryCrafting createAdvancedRecipeGrid() {
        InventoryCrafting crafting = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer playerIn) {
                return false;
            }
        }, 3, 3);
        crafting.setInventorySlotContents(0, new ItemStack(Items.LEATHER));
        crafting.setInventorySlotContents(1, new ItemStack(ModItems.BACKPACK));
        crafting.setInventorySlotContents(2, new ItemStack(Items.LEATHER));
        crafting.setInventorySlotContents(3, new ItemStack(Items.LEATHER));
        crafting.setInventorySlotContents(4, new ItemStack(ModItems.BACKPACK));
        crafting.setInventorySlotContents(5, new ItemStack(Items.LEATHER));
        crafting.setInventorySlotContents(6, new ItemStack(Items.LEAD));
        crafting.setInventorySlotContents(8, new ItemStack(Items.LEAD));
        return crafting;
    }

    private static void verifyTexture(String name) {
        try {
            File file = new File("src/main/resources/assets/lisbam_pastoral_economy/textures/items", name);
            BufferedImage image = ImageIO.read(file);
            check(image != null && image.getWidth() == 16 && image.getHeight() == 16,
                    name + " must be a readable 16x16 PNG");
            check(image.getColorModel().hasAlpha() && (image.getRGB(0, 0) >>> 24) == 0,
                    name + " must have transparent item padding");
        } catch (java.io.IOException exception) {
            throw new AssertionError("Cannot read " + name, exception);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
