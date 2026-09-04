package lisbam.pastoraleconomy.block;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.gui.GuiIds;
import lisbam.pastoraleconomy.tile.TileCrabTrap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import lisbam.pastoraleconomy.item.ModItems;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Collectible water-side production block for the batch 12/13 crab trap. */
public final class BlockCrabTrap extends Block {
    BlockCrabTrap() {
        super(Material.WOOD);
        setRegistryName(LisBamPastoralEconomy.MODID, "crab_trap");
        setUnlocalizedName(LisBamPastoralEconomy.MODID + ".crab_trap");
        setHardness(2.5F);
        setResistance(5.0F);
        setCreativeTab(ModItems.CREATIVE_TAB);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileCrabTrap();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(LisBamPastoralEconomy.INSTANCE, GuiIds.CRAB_TRAP, world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof TileCrabTrap) {
            TileCrabTrap trap = (TileCrabTrap) tile;
            for (int index = 0; index < trap.getSizeInventory(); index++) {
                dropStack(world, pos, trap.removeStackFromSlot(index));
            }
            dropStack(world, pos, trap.getPendingLoot());
        }
        super.breakBlock(world, pos, state);
    }

    private static void dropStack(World world, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        EntityItem item = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                stack.copy());
        item.setDefaultPickupDelay();
        world.spawnEntity(item);
    }
}
