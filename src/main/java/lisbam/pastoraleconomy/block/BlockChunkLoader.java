package lisbam.pastoraleconomy.block;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.chunkloader.ChunkLoaderRules;
import lisbam.pastoraleconomy.chunkloader.ChunkLoaderService;
import lisbam.pastoraleconomy.item.ModItems;
import lisbam.pastoraleconomy.tile.TileChunkLoader;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** A redstone-controlled Forge ticket holder for exactly its own chunk. */
public final class BlockChunkLoader extends Block {
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    BlockChunkLoader() {
        super(Material.ROCK);
        setDefaultState(blockState.getBaseState().withProperty(POWERED, Boolean.FALSE));
        setRegistryName(LisBamPastoralEconomy.MODID, "chunk_loader");
        setUnlocalizedName(LisBamPastoralEconomy.MODID + ".chunk_loader");
        setHardness(3.0F);
        setResistance(12.0F);
        setCreativeTab(ModItems.CREATIVE_TAB);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileChunkLoader();
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        refreshRedstoneState(world, pos);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer,
                                ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        refreshRedstoneState(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, block, fromPos);
        refreshRedstoneState(world, pos);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof TileChunkLoader) {
            ChunkLoaderService.onChunkLoaderBroken(world, pos, (TileChunkLoader) tile);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(POWERED).booleanValue() ? ChunkLoaderRules.ACTIVE_LIGHT_LEVEL : 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(POWERED, Boolean.valueOf((meta & 1) != 0));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(POWERED).booleanValue() ? 1 : 0;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POWERED);
    }

    private static void refreshRedstoneState(World world, BlockPos pos) {
        if (!world.isRemote && world.getTileEntity(pos) instanceof TileChunkLoader) {
            ChunkLoaderService.updateForRedstone(world, pos, (TileChunkLoader) world.getTileEntity(pos));
        }
    }
}
