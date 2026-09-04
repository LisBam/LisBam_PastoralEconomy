package lisbam.pastoraleconomy.crabtrap;

import lisbam.pastoraleconomy.tile.TileCrabTrap;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
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
        assertEquals(100, CrabTrapRules.rollRoundWaitTicks(0, false, () -> 100), "base minimum");
        assertEquals(600, CrabTrapRules.rollRoundWaitTicks(0, false, () -> 600), "base maximum");
        assertEquals(200, CrabTrapRules.rollRoundWaitTicks(1, false, () -> 300), "Lure I reduction");
        assertEquals(200, CrabTrapRules.rollRoundWaitTicks(2, false, () -> 400), "Lure II reduction");
        assertEquals(200, CrabTrapRules.rollRoundWaitTicks(3, false, () -> 500), "Lure III reduction");
        final int[] draws = new int[] { 100, 400 };
        final int[] cursor = new int[] { 0 };
        assertEquals(300, CrabTrapRules.rollRoundWaitTicks(1, false, () -> draws[cursor[0]++]),
                "zero adjusted Lure wait must redraw instead of clamp");
        assertEquals(51, CrabTrapRules.rollRoundWaitTicks(0, true, () -> 101),
                "bait must ceiling-divide odd waits by two");
        assertEquals(100, CrabTrapRules.rollRoundWaitTicks(1, true, () -> 300),
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
        assertEquals(1, trap.getSlotsForFace(EnumFacing.UP).length, "top hopper input slots");
        assertEquals(TileCrabTrap.BAIT_SLOT, trap.getSlotsForFace(EnumFacing.UP)[0], "top input is bait only");
        assertEquals(18, trap.getSlotsForFace(EnumFacing.DOWN).length, "bottom hopper output slots");
        assertTrue(trap.canInsertItem(TileCrabTrap.BAIT_SLOT, new ItemStack(Items.BEEF), EnumFacing.NORTH),
                "side hopper accepts raw meat bait");
        assertFalse(trap.canInsertItem(TileCrabTrap.ROD_SLOT, new ItemStack(Items.FISHING_ROD), EnumFacing.UP),
                "hopper cannot insert rod");
        assertFalse(trap.canInsertItem(TileCrabTrap.BAIT_SLOT, new ItemStack(Items.FISH, 1, 0), EnumFacing.UP),
                "hopper cannot insert caught fish as bait");
        assertTrue(trap.canExtractItem(TileCrabTrap.FIRST_HARVEST_SLOT, new ItemStack(Items.FISH), EnumFacing.DOWN),
                "bottom hopper extracts harvest");
        assertFalse(trap.canExtractItem(TileCrabTrap.BAIT_SLOT, new ItemStack(Items.BEEF), EnumFacing.DOWN),
                "bottom hopper cannot extract bait");
        assertFalse(trap.canExtractItem(TileCrabTrap.ROD_SLOT, new ItemStack(Items.FISHING_ROD), EnumFacing.DOWN),
                "bottom hopper cannot extract rod");

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
