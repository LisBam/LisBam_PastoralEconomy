package lisbam.pastoraleconomy.agriculture;

import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Random;

/** Shared exact crop identities and frozen random formulas for batch 06. */
public final class AgricultureRules {
    public enum Crop {
        WHEAT,
        CARROT,
        POTATO,
        BEETROOT,
        NETHER_WART,
        MELON,
        COCOA,
        PUMPKIN
    }

    private AgricultureRules() {
    }

    public static Crop getMatureHarvestCrop(IBlockState state) {
        if (state.getBlock() == Blocks.WHEAT && isMatureCrop(state)) {
            return Crop.WHEAT;
        }
        if (state.getBlock() == Blocks.CARROTS && isMatureCrop(state)) {
            return Crop.CARROT;
        }
        if (state.getBlock() == Blocks.POTATOES && isMatureCrop(state)) {
            return Crop.POTATO;
        }
        if (state.getBlock() == Blocks.BEETROOTS && isMatureCrop(state)) {
            return Crop.BEETROOT;
        }
        if (state.getBlock() == Blocks.NETHER_WART
                && state.getValue(BlockNetherWart.AGE).intValue() == 3) {
            return Crop.NETHER_WART;
        }
        if (state.getBlock() == Blocks.COCOA && state.getValue(BlockCocoa.AGE).intValue() == 2) {
            return Crop.COCOA;
        }
        if (state.getBlock() == Blocks.MELON_BLOCK) {
            return Crop.MELON;
        }
        if (state.getBlock() == Blocks.PUMPKIN) {
            return Crop.PUMPKIN;
        }
        return null;
    }

    public static Crop getPlacedCrop(IBlockState state) {
        if (state.getBlock() == Blocks.WHEAT) {
            return Crop.WHEAT;
        }
        if (state.getBlock() == Blocks.CARROTS) {
            return Crop.CARROT;
        }
        if (state.getBlock() == Blocks.POTATOES) {
            return Crop.POTATO;
        }
        if (state.getBlock() == Blocks.BEETROOTS) {
            return Crop.BEETROOT;
        }
        if (state.getBlock() == Blocks.NETHER_WART) {
            return Crop.NETHER_WART;
        }
        if (state.getBlock() == Blocks.COCOA) {
            return Crop.COCOA;
        }
        if (state.getBlock() == Blocks.MELON_STEM) {
            return Crop.MELON;
        }
        if (state.getBlock() == Blocks.PUMPKIN_STEM) {
            return Crop.PUMPKIN;
        }
        return null;
    }

    /** Existing crop scope whose zero-hardness harvests need the hoe's missing vanilla wear restored. */
    public static boolean shouldConsumeHoeCropDurability(IBlockState state, float blockHardness) {
        return state != null && blockHardness == 0.0F
                && (getPlacedCrop(state) != null || getMatureHarvestCrop(state) != null);
    }

    public static boolean supportsFineCultivation(Crop crop) {
        return crop == Crop.WHEAT || crop == Crop.CARROT || crop == Crop.POTATO || crop == Crop.BEETROOT;
    }

    /** Exact authoritative age-zero state written by a confirmed Fine Cultivation replant. */
    public static IBlockState getFineCultivationReplantState(Crop crop) {
        if (crop == Crop.WHEAT) {
            return Blocks.WHEAT.getDefaultState();
        }
        if (crop == Crop.CARROT) {
            return Blocks.CARROTS.getDefaultState();
        }
        if (crop == Crop.POTATO) {
            return Blocks.POTATOES.getDefaultState();
        }
        if (crop == Crop.BEETROOT) {
            return Blocks.BEETROOTS.getDefaultState();
        }
        return null;
    }

    public static Item getPrimaryDrop(Crop crop) {
        switch (crop) {
            case WHEAT:
                return Items.WHEAT;
            case CARROT:
                return Items.CARROT;
            case POTATO:
                return Items.POTATO;
            case BEETROOT:
                return Items.BEETROOT;
            case NETHER_WART:
                return Items.NETHER_WART;
            case MELON:
                return Items.MELON;
            default:
                return null;
        }
    }

    public static Item getReplantSeed(Crop crop) {
        switch (crop) {
            case WHEAT:
                return Items.WHEAT_SEEDS;
            case CARROT:
                return Items.CARROT;
            case POTATO:
                return Items.POTATO;
            case BEETROOT:
                return Items.BEETROOT_SEEDS;
            default:
                return null;
        }
    }

    public static ItemStack createHarvestBonus(Crop crop, int level, Random random) {
        if (level <= 0) {
            return ItemStack.EMPTY;
        }
        Item primary = getPrimaryDrop(crop);
        int amount = 0;
        switch (crop) {
            case CARROT:
            case POTATO:
                amount = binomial(random, level * 2, 4, 7);
                break;
            case WHEAT:
            case BEETROOT:
                amount = binomial(random, level * 2, 1, 2);
                break;
            case NETHER_WART:
                amount = random.nextInt(level * 2 + 1);
                break;
            case MELON:
                amount = level + random.nextInt(level + 1);
                break;
            case COCOA:
                return new ItemStack(Items.DYE, level, 3);
            default:
                return ItemStack.EMPTY;
        }
        return amount > 0 && primary != null ? new ItemStack(primary, amount) : ItemStack.EMPTY;
    }

    public static boolean rollFineCultivation(int level, Random random) {
        return level > 0 && random.nextInt(100) < level * 25;
    }

    public static boolean rollPastoralFavor(int level, Random random) {
        return getPastoralFavorChancePercent(level) > random.nextInt(100);
    }

    /** Frozen Pastoral Favor experience chances for levels I through IV. */
    public static int getPastoralFavorChancePercent(int level) {
        switch (level) {
            case 1:
                return 20;
            case 2:
                return 40;
            case 3:
                return 60;
            default:
                return level >= 4 ? 80 : 0;
        }
    }

    /** Extra shearing output uses the same two 4/7 trials per Harvest level as root crops. */
    public static int rollShearingHarvestBonus(int level, Random random) {
        return level <= 0 ? 0 : binomial(random, level * 2, 4, 7);
    }

    private static boolean isMatureCrop(IBlockState state) {
        return state.getBlock() instanceof BlockCrops && ((BlockCrops) state.getBlock()).isMaxAge(state);
    }

    private static int binomial(Random random, int trials, int numerator, int denominator) {
        int amount = 0;
        for (int trial = 0; trial < trials; trial++) {
            if (random.nextInt(denominator) < numerator) {
                amount++;
            }
        }
        return amount;
    }
}
