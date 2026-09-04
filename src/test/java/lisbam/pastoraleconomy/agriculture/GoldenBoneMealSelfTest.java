package lisbam.pastoraleconomy.agriculture;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** Deterministic bounds and target-classification checks for golden bone meal. */
public final class GoldenBoneMealSelfTest {
    private GoldenBoneMealSelfTest() {
    }

    public static void main(String[] args) {
        Bootstrap.register();
        testCultivatedCropClassification();
        testWildGrowthClassification();
        testFiveByFiveArea();
        System.out.println("GoldenBoneMealSelfTest: PASS");
    }

    private static void testCultivatedCropClassification() {
        assertTrue(GoldenBoneMealRules.isCultivatedCrop(Blocks.WHEAT.getDefaultState()), "wheat is a crop");
        assertTrue(GoldenBoneMealRules.isCultivatedCrop(Blocks.MELON_STEM.getDefaultState()), "melon stem is a crop");
        assertTrue(GoldenBoneMealRules.isCultivatedCrop(Blocks.NETHER_WART.getDefaultState()), "nether wart is a crop");
        assertTrue(GoldenBoneMealRules.isCultivatedCrop(Blocks.COCOA.getDefaultState()), "cocoa is a crop");
        assertFalse(GoldenBoneMealRules.isCultivatedCrop(Blocks.GRASS.getDefaultState()), "grass is not a crop");
    }

    private static void testWildGrowthClassification() {
        assertTrue(GoldenBoneMealRules.isWildGrowthTarget(Blocks.GRASS.getDefaultState()), "grass is wild growth");
        assertTrue(GoldenBoneMealRules.isWildGrowthTarget(Blocks.RED_FLOWER.getDefaultState()), "flowers are wild growth");
        assertTrue(GoldenBoneMealRules.isWildGrowthTarget(Blocks.DOUBLE_PLANT.getDefaultState()),
                "double plants are wild growth");
        assertFalse(GoldenBoneMealRules.isWildGrowthTarget(Blocks.WHEAT.getDefaultState()), "wheat is not wild growth");
    }

    private static void testFiveByFiveArea() {
        BlockPos center = new BlockPos(12, 64, -3);
        List<BlockPos> area = GoldenBoneMealRules.getArea(center);
        assertEquals(25, area.size(), "5x5 area has 25 positions");
        assertTrue(area.contains(center), "area includes center");
        assertTrue(area.contains(new BlockPos(10, 64, -5)), "area includes north-west corner");
        assertTrue(area.contains(new BlockPos(14, 64, -1)), "area includes south-east corner");
        assertFalse(area.contains(new BlockPos(15, 64, -3)), "area excludes positions outside radius");
        assertFalse(area.contains(new BlockPos(12, 65, -3)), "area never changes height");
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
