package lisbam.pastoraleconomy.item;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.agriculture.GoldenBoneMealRules;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.EntityLivingBase;
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
import net.minecraftforge.oredict.OreDictionary;

/**
 * One-use enhanced bone meal. World mutations remain on the logical server;
 * the original ItemDye application path keeps Forge's normal bonemeal hooks.
 */
public final class ItemGoldenBoneMeal extends Item {
    private static final BonemealApplication DISPENSER_BONEMEAL_APPLICATION = new BonemealApplication() {
        @Override
        public boolean apply(World world, BlockPos pos) {
            return applyVanillaBoneMeal(world, pos);
        }
    };

    private static final BehaviorDefaultDispenseItem DISPENSE_BEHAVIOR = new BehaviorDefaultDispenseItem() {
        @Override
        protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
            BlockPos target = source.getBlockPos().offset(
                    source.getBlockState().getValue(BlockDispenser.FACING));
            if (applyGoldenBonemeal(source.getWorld(), target, DISPENSER_BONEMEAL_APPLICATION)) {
                stack.shrink(1);
                source.getWorld().playEvent(2005, target, 0);
                return stack;
            }
            return super.dispenseStack(source, stack);
        }
    };

    public ItemGoldenBoneMeal() {
        setRegistryName(LisBamPastoralEconomy.MODID, "golden_bone_meal");
        setUnlocalizedName(LisBamPastoralEconomy.MODID + ".golden_bone_meal");
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.MATERIALS);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!player.canPlayerEdit(pos.offset(facing), facing, player.getHeldItem(hand))) {
            return EnumActionResult.FAIL;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        boolean changed = applyGoldenBonemeal(world, pos, new PlayerBonemealApplication(player, hand));

        if (!changed) {
            return EnumActionResult.FAIL;
        }
        if (!player.capabilities.isCreativeMode) {
            player.getHeldItem(hand).shrink(1);
        }
        world.playEvent(2005, pos, 0);
        return EnumActionResult.SUCCESS;
    }

    /**
     * Adds this item to standard white-dye recipe lookups and assigns its own
     * enhanced growth behavior when a dispenser targets a block.
     */
    public static void registerCompatibility() {
        OreDictionary.registerOre("dyeWhite", new ItemStack(ModItems.GOLDEN_BONE_MEAL));
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(ModItems.GOLDEN_BONE_MEAL, DISPENSE_BEHAVIOR);
    }

    /** Preserves ordinary white-dye behavior when used on a dyeable sheep. */
    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target,
                                            EnumHand hand) {
        ItemStack vanillaWhiteDye = createVanillaBoneMeal();
        boolean handled = Items.DYE.itemInteractionForEntity(vanillaWhiteDye, player, target, hand);
        if (handled && vanillaWhiteDye.getCount() == 0) {
            stack.shrink(1);
        }
        return handled;
    }

    private static boolean applyGoldenBonemeal(World world, BlockPos pos, BonemealApplication application) {
        IBlockState targetState = world.getBlockState(pos);
        if (GoldenBoneMealRules.isCultivatedCrop(targetState)) {
            boolean changed = matureCenterCrop(world, pos, application);
            return growSurroundingCrops(world, pos, application) || changed;
        }
        if (GoldenBoneMealRules.isWildGrowthTarget(targetState)) {
            BlockPos grassLevel = targetState.getBlock() == Blocks.GRASS ? pos : pos.down();
            return growNaturalVegetation(world, grassLevel, application);
        }
        return application.apply(world, pos);
    }

    /** Reapply the original bone-meal growth until the selected crop no longer advances. */
    private static boolean matureCenterCrop(World world, BlockPos pos, BonemealApplication application) {
        boolean changed = false;
        for (int attempt = 0; attempt < GoldenBoneMealRules.MAX_CENTER_GROWTH_APPLICATIONS; attempt++) {
            IBlockState before = world.getBlockState(pos);
            if (!GoldenBoneMealRules.isCultivatedCrop(before) || !application.apply(world, pos)) {
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
    private static boolean growSurroundingCrops(World world, BlockPos center, BonemealApplication application) {
        boolean changed = false;
        for (BlockPos target : GoldenBoneMealRules.getArea(center)) {
            if (target.equals(center) || !GoldenBoneMealRules.isCultivatedCrop(world.getBlockState(target))) {
                continue;
            }
            changed |= application.apply(world, target);
        }
        return changed;
    }

    /** Grass under the clicked grass block or flower gets the same expanded 5x5 treatment. */
    private static boolean growNaturalVegetation(World world, BlockPos grassLevel, BonemealApplication application) {
        boolean changed = false;
        for (BlockPos target : GoldenBoneMealRules.getArea(grassLevel)) {
            if (world.getBlockState(target).getBlock() == Blocks.GRASS) {
                changed |= application.apply(world, target);
            }
        }
        return changed;
    }

    /**
     * Calling ItemDye retains the ordinary 1.12.2 IGrowable behavior, Forge
     * onApplyBonemeal hook and any other mod's bonemeal compatibility.
     */
    private static boolean applyVanillaBoneMeal(EntityPlayer player, World world, BlockPos pos, EnumHand hand) {
        return ItemDye.applyBonemeal(createVanillaBoneMeal(), world, pos, player, hand);
    }

    /** The no-player overload lets the vanilla dispenser FakePlayer route run unchanged. */
    private static boolean applyVanillaBoneMeal(World world, BlockPos pos) {
        return ItemDye.applyBonemeal(createVanillaBoneMeal(), world, pos);
    }

    private static ItemStack createVanillaBoneMeal() {
        return new ItemStack(Items.DYE, 1, EnumDyeColor.WHITE.getDyeDamage());
    }

    private interface BonemealApplication {
        boolean apply(World world, BlockPos pos);
    }

    private static final class PlayerBonemealApplication implements BonemealApplication {
        private final EntityPlayer player;
        private final EnumHand hand;

        private PlayerBonemealApplication(EntityPlayer player, EnumHand hand) {
            this.player = player;
            this.hand = hand;
        }

        @Override
        public boolean apply(World world, BlockPos pos) {
            return applyVanillaBoneMeal(player, world, pos, hand);
        }
    }
}
