package lisbam.pastoraleconomy.item;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.agriculture.GoldenBoneMealRules;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * One-use enhanced bone meal. World mutations remain on the logical server;
 * the original ItemDye application path keeps Forge's normal bonemeal hooks.
 */
public final class ItemGoldenBoneMeal extends Item {
    public ItemGoldenBoneMeal() {
        setRegistryName(LisBamPastoralEconomy.MODID, "golden_bone_meal");
        setUnlocalizedName(LisBamPastoralEconomy.MODID + ".golden_bone_meal");
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.MATERIALS);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        IBlockState targetState = world.getBlockState(pos);
        boolean changed;
        if (GoldenBoneMealRules.isCultivatedCrop(targetState)) {
            changed = matureCenterCrop(player, world, pos, hand);
            changed |= growSurroundingCrops(player, world, pos, hand);
        } else if (GoldenBoneMealRules.isWildGrowthTarget(targetState)) {
            BlockPos grassLevel = targetState.getBlock() == Blocks.GRASS ? pos : pos.down();
            changed = growNaturalVegetation(player, world, grassLevel, hand);
        } else {
            changed = applyVanillaBoneMeal(player, world, pos, hand);
        }

        if (!changed) {
            return EnumActionResult.FAIL;
        }
        if (!player.capabilities.isCreativeMode) {
            player.getHeldItem(hand).shrink(1);
        }
        world.playEvent(2005, pos, 0);
        return EnumActionResult.SUCCESS;
    }

    /** Reapply the original bone-meal growth until the selected crop no longer advances. */
    private static boolean matureCenterCrop(EntityPlayer player, World world, BlockPos pos, EnumHand hand) {
        boolean changed = false;
        for (int application = 0; application < GoldenBoneMealRules.MAX_CENTER_GROWTH_APPLICATIONS; application++) {
            IBlockState before = world.getBlockState(pos);
            if (!GoldenBoneMealRules.isCultivatedCrop(before) || !applyVanillaBoneMeal(player, world, pos, hand)) {
                break;
            }
            changed = true;
            if (before.equals(world.getBlockState(pos))) {
                break;
            }
        }
        return changed;
    }

    /** Every non-center cultivated crop receives exactly one normal bone-meal application. */
    private static boolean growSurroundingCrops(EntityPlayer player, World world, BlockPos center, EnumHand hand) {
        boolean changed = false;
        for (BlockPos target : GoldenBoneMealRules.getArea(center)) {
            if (target.equals(center) || !GoldenBoneMealRules.isCultivatedCrop(world.getBlockState(target))) {
                continue;
            }
            changed |= applyVanillaBoneMeal(player, world, target, hand);
        }
        return changed;
    }

    /** Grass under the clicked grass block or flower gets the same expanded 5x5 treatment. */
    private static boolean growNaturalVegetation(EntityPlayer player, World world, BlockPos grassLevel, EnumHand hand) {
        boolean changed = false;
        for (BlockPos target : GoldenBoneMealRules.getArea(grassLevel)) {
            if (world.getBlockState(target).getBlock() == Blocks.GRASS) {
                changed |= applyVanillaBoneMeal(player, world, target, hand);
            }
        }
        return changed;
    }

    /**
     * Calling ItemDye retains the ordinary 1.12.2 IGrowable behavior, Forge
     * onApplyBonemeal hook and any other mod's bonemeal compatibility.
     */
    private static boolean applyVanillaBoneMeal(EntityPlayer player, World world, BlockPos pos, EnumHand hand) {
        ItemStack vanillaBoneMeal = new ItemStack(Items.DYE, 1, EnumDyeColor.WHITE.getDyeDamage());
        return ItemDye.applyBonemeal(vanillaBoneMeal, world, pos, player, hand);
    }
}
