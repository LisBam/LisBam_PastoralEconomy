package lisbam.pastoraleconomy.agriculture;

import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockStem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure target classification and bounded area rules for golden bone meal. */
public final class GoldenBoneMealRules {
    public static final int RADIUS = 2;
    public static final int MAX_CENTER_GROWTH_APPLICATIONS = 8;

    private GoldenBoneMealRules() {
    }

    /** Vanilla cultivated crops whose center block is advanced to its final growth state. */
    public static boolean isCultivatedCrop(IBlockState state) {
        if (state == null) {
            return false;
        }
        return state.getBlock() instanceof BlockCrops
                || state.getBlock() instanceof BlockStem
                || state.getBlock() instanceof BlockNetherWart
                || state.getBlock() instanceof BlockCocoa;
    }

    /** Grass and flowers use the larger natural-vegetation application path. */
    public static boolean isWildGrowthTarget(IBlockState state) {
        return state != null && (state.getBlock() == Blocks.GRASS
                || state.getBlock() instanceof BlockFlower || state.getBlock() instanceof BlockDoublePlant);
    }

    /** A deterministic 5x5 X/Z square, including the center position. */
    public static List<BlockPos> getArea(BlockPos center) {
        if (center == null) {
            return Collections.emptyList();
        }
        List<BlockPos> positions = new ArrayList<BlockPos>((RADIUS * 2 + 1) * (RADIUS * 2 + 1));
        for (int offsetZ = -RADIUS; offsetZ <= RADIUS; offsetZ++) {
            for (int offsetX = -RADIUS; offsetX <= RADIUS; offsetX++) {
                positions.add(center.add(offsetX, 0, offsetZ));
            }
        }
        return Collections.unmodifiableList(positions);
    }
}
