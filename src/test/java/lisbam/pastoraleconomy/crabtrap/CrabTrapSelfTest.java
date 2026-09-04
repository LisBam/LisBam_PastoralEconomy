package lisbam.pastoraleconomy.crabtrap;

import lisbam.pastoraleconomy.tile.TileCrabTrap;
import lisbam.pastoraleconomy.gui.ContainerCrabTrap;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Standalone deterministic regression checks for batch 12/13 frozen rules. */
public final class CrabTrapSelfTest {
    private CrabTrapSelfTest() {
    }

    public static void main(String[] args) {
        Bootstrap.register();
        GameRegistry.registerTileEntity(TileCrabTrap.class,
                new ResourceLocation("minecraft", "crab_trap_self_test"));
        testAllowedBait();
        testWaitRulesAndRedraw();
        testVanillaFishingRootWeights();
        testInventorySidesAndCapacity();
        testInventoryNbtRoundTrip();
        System.out.println("CrabTrapSelfTest: PASS");
    }

    private static void testAllowedBait() {
        assertTrue(CrabTrapRules.isRawMeatBait(new ItemStack(Items.BEEF)), "raw beef must be bait");
        assertTrue(CrabTrapRules.isRawMeatBait(new ItemStack(Items.PORKCHOP)), "raw porkchop must be bait");
        assertTrue(CrabTrapRules.isRawMeatBait(new ItemStack(Items.CHICKEN)), "raw chicken must be bait");
        assertTrue(CrabTrapRules.isRawMeatBait(new ItemStack(Items.MUTTON)), "raw mutton must be bait");
        assertTrue(CrabTrapRules.isRawMeatBait(new ItemStack(Items.RABBIT)), "raw rabbit must be bait");
        assertFalse(CrabTrapRules.isRawMeatBait(new ItemStack(Items.FISH, 1, 0)), "raw fish must not be bait");
        assertFalse(CrabTrapRules.isRawMeatBait(new ItemStack(Items.FISH, 1, 1)), "raw salmon must not be bait");
        assertFalse(CrabTrapRules.isRawMeatBait(new ItemStack(Items.FISH, 1, 2)), "clownfish must not be bait");
        assertFalse(CrabTrapRules.isRawMeatBait(new ItemStack(Items.FISH, 1, 3)), "pufferfish must not be bait");
    }

    private static void testWaitRulesAndRedraw() {
        assertEquals(2000, CrabTrapRules.rollRoundWaitTicks(0, false, () -> 2000), "base minimum");
        assertEquals(12000, CrabTrapRules.rollRoundWaitTicks(0, false, () -> 12000), "base maximum");
        assertEquals(2000, CrabTrapRules.rollRoundWaitTicks(1, false, () -> 4000), "Lure I reduction");
        assertEquals(2000, CrabTrapRules.rollRoundWaitTicks(2, false, () -> 6000), "Lure II reduction");
        assertEquals(2000, CrabTrapRules.rollRoundWaitTicks(3, false, () -> 8000), "Lure III reduction");
        final int[] draws = new int[] { 2000, 8000 };
        final int[] cursor = new int[] { 0 };
        assertEquals(6000, CrabTrapRules.rollRoundWaitTicks(1, false, () -> draws[cursor[0]++]),
                "zero adjusted Lure wait must redraw instead of clamp");
        assertEquals(1001, CrabTrapRules.rollRoundWaitTicks(0, true, () -> 2001),
                "bait must ceiling-divide odd waits by two");
        assertEquals(1000, CrabTrapRules.rollRoundWaitTicks(1, true, () -> 4000),
                "bait applies only after Lure adjustment");
    }

    private static void testVanillaFishingRootWeights() {
        assertWeights(0, 85, 10, 5);
        assertWeights(1, 84, 8, 7);
        assertWeights(2, 83, 6, 9);
        assertWeights(3, 82, 4, 11);
    }

    private static void assertWeights(int luck, int fish, int junk, int treasure) {
        assertEquals(fish, CrabTrapRules.getFishCategoryWeight(luck), "fish root weight for Luck " + luck);
        assertEquals(junk, CrabTrapRules.getJunkCategoryWeight(luck), "junk root weight for Luck " + luck);
        assertEquals(treasure, CrabTrapRules.getTreasureCategoryWeight(luck), "treasure root weight for Luck " + luck);
    }

    private static void testInventorySidesAndCapacity() {
        TileCrabTrap trap = new TileCrabTrap();
        assertEquals(20, trap.getSizeInventory(), "total slots");
        for (EnumFacing side : EnumFacing.values()) {
            assertEquals(20, trap.getSlotsForFace(side).length, "every hopper side exposes all slots");
        }
        ItemStack dirt = new ItemStack(net.minecraft.init.Blocks.DIRT);
        assertTrue(trap.isItemValidForSlot(TileCrabTrap.ROD_SLOT, new ItemStack(Items.FISHING_ROD)),
                "rod slot accepts fishing rods");
        assertFalse(trap.isItemValidForSlot(TileCrabTrap.ROD_SLOT, dirt), "rod slot rejects ordinary storage");
        assertTrue(trap.isItemValidForSlot(TileCrabTrap.BAIT_SLOT, new ItemStack(Items.BEEF)),
                "bait slot accepts raw meat");
        assertFalse(trap.isItemValidForSlot(TileCrabTrap.BAIT_SLOT, new ItemStack(Items.FISH, 1, 0)),
                "bait slot rejects fishing output");
        assertFalse(trap.canInsertItem(TileCrabTrap.ROD_SLOT, dirt, EnumFacing.NORTH),
                "side hopper respects rod-slot validation");
        assertFalse(trap.canInsertItem(TileCrabTrap.BAIT_SLOT, dirt, EnumFacing.NORTH),
                "side hopper respects bait-slot validation");
        ContainerCrabTrap container = new ContainerCrabTrap(new InventoryPlayer(null), trap);
        assertFalse(container.inventorySlots.get(TileCrabTrap.ROD_SLOT).isItemValid(dirt),
                "GUI rod slot rejects ordinary storage");
        assertFalse(container.inventorySlots.get(TileCrabTrap.BAIT_SLOT).isItemValid(dirt),
                "GUI bait slot rejects ordinary storage");
        assertTrue(container.inventorySlots.get(TileCrabTrap.ROD_SLOT).isItemValid(new ItemStack(Items.FISHING_ROD)),
                "GUI rod slot accepts fishing rods");
        assertTrue(container.inventorySlots.get(TileCrabTrap.BAIT_SLOT).isItemValid(new ItemStack(Items.BEEF)),
                "GUI bait slot accepts raw meat");
        trap.setInventorySlotContents(TileCrabTrap.ROD_SLOT, dirt);
        trap.setInventorySlotContents(TileCrabTrap.BAIT_SLOT, dirt);
        assertTrue(trap.getStackInSlot(TileCrabTrap.ROD_SLOT).isEmpty(), "direct rod insert rejects ordinary storage");
        assertTrue(trap.getStackInSlot(TileCrabTrap.BAIT_SLOT).isEmpty(), "direct bait insert rejects ordinary storage");
        trap.setInventorySlotContents(TileCrabTrap.FIRST_HARVEST_SLOT, dirt);
        assertTrue(trap.getStackInSlot(TileCrabTrap.FIRST_HARVEST_SLOT).getItem() == dirt.getItem(),
                "direct harvest storage accepts arbitrary items");
        trap.setInventorySlotContents(TileCrabTrap.FIRST_HARVEST_SLOT, ItemStack.EMPTY);
        assertTrue(trap.canExtractItem(TileCrabTrap.ROD_SLOT, new ItemStack(Items.FISHING_ROD), EnumFacing.DOWN),
                "rod slot remains extractable");
        assertTrue(trap.canExtractItem(TileCrabTrap.BAIT_SLOT, new ItemStack(Items.BEEF), EnumFacing.DOWN),
                "bait slot remains extractable");
        for (int index = TileCrabTrap.FIRST_HARVEST_SLOT; index < TileCrabTrap.SLOT_COUNT; index++) {
            assertTrue(trap.isItemValidForSlot(index, dirt), "harvest storage accepts arbitrary items");
            assertTrue(trap.canInsertItem(index, dirt, EnumFacing.NORTH), "side hopper inserts into harvest storage");
            assertTrue(trap.canExtractItem(index, new ItemStack(net.minecraft.init.Blocks.DIRT), EnumFacing.DOWN),
                    "bottom hopper extracts storage");
        }

        for (int index = TileCrabTrap.FIRST_HARVEST_SLOT; index < TileCrabTrap.SLOT_COUNT; index++) {
            trap.setInventorySlotContents(index, new ItemStack(Items.BONE, 64));
        }
        assertFalse(trap.canFullyInsertIntoHarvest(new ItemStack(Items.BONE)), "full harvest storage blocks a result");
        trap.setInventorySlotContents(TileCrabTrap.FIRST_HARVEST_SLOT, new ItemStack(Items.BONE, 63));
        assertTrue(trap.canFullyInsertIntoHarvest(new ItemStack(Items.BONE)), "stack room accepts a complete result");
    }

    private static void testInventoryNbtRoundTrip() {
        TileCrabTrap original = new TileCrabTrap();
        original.setPos(BlockPos.ORIGIN);
        original.setInventorySlotContents(TileCrabTrap.ROD_SLOT, new ItemStack(Items.FISHING_ROD));
        original.setInventorySlotContents(TileCrabTrap.BAIT_SLOT, new ItemStack(Items.RABBIT, 7));
        original.setInventorySlotContents(TileCrabTrap.FIRST_HARVEST_SLOT, new ItemStack(Items.FISH, 10, 3));
        NBTTagCompound serialized = original.writeToNBT(new NBTTagCompound());
        TileCrabTrap restored = new TileCrabTrap();
        restored.readFromNBT(serialized);
        assertTrue(restored.getStackInSlot(TileCrabTrap.ROD_SLOT).getItem() == Items.FISHING_ROD, "rod NBT round trip");
        assertEquals(7, restored.getStackInSlot(TileCrabTrap.BAIT_SLOT).getCount(), "bait NBT round trip");
        assertEquals(3, restored.getStackInSlot(TileCrabTrap.FIRST_HARVEST_SLOT).getMetadata(), "fish metadata NBT round trip");
        assertEquals(10, restored.getStackInSlot(TileCrabTrap.FIRST_HARVEST_SLOT).getCount(), "harvest count NBT round trip");

        NBTTagCompound legacyRodSlot = serialized.getTagList("inventory", 10).getCompoundTagAt(0);
        new ItemStack(net.minecraft.init.Blocks.DIRT).writeToNBT(legacyRodSlot);
        legacyRodSlot.setByte("slot", (byte) TileCrabTrap.ROD_SLOT);
        TileCrabTrap legacyRestored = new TileCrabTrap();
        legacyRestored.readFromNBT(serialized);
        assertTrue(legacyRestored.getStackInSlot(TileCrabTrap.ROD_SLOT).getItem() == net.minecraft.item.Item.getItemFromBlock(
                net.minecraft.init.Blocks.DIRT), "legacy invalid functional-slot item remains removable after load");
        assertFalse(legacyRestored.removeStackFromSlot(TileCrabTrap.ROD_SLOT).isEmpty(),
                "legacy invalid functional-slot item can be removed after load");

        NBTTagCompound pendingTag = new NBTTagCompound();
        new ItemStack(Items.DYE, 10, 0).writeToNBT(pendingTag);
        serialized.setTag("pendingLoot", pendingTag);
        TileCrabTrap pendingRestored = new TileCrabTrap();
        pendingRestored.readFromNBT(serialized);
        assertTrue(pendingRestored.hasPendingLoot(), "pending loot NBT must restore its blocked state");
        assertEquals(10, pendingRestored.getPendingLoot().getCount(), "pending loot count NBT round trip");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }
}
