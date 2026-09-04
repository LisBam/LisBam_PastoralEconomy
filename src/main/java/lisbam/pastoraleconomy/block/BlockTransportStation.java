package lisbam.pastoraleconomy.block;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.gui.GuiIds;
import lisbam.pastoraleconomy.item.ModItems;
import lisbam.pastoraleconomy.tile.TileTransportStation;
import lisbam.pastoraleconomy.transport.StarterTransportItemData;
import lisbam.pastoraleconomy.transport.TransportService;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Collectible player-built transport node. The world registry owns its UUID. */
public final class BlockTransportStation extends Block {
    /** Explicit display key: neither block nor ItemBlock may append a second suffix. */
    public static final String DISPLAY_NAME_KEY = "tile." + LisBamPastoralEconomy.MODID + ".transport_station";

    BlockTransportStation() {
        super(Material.ROCK);
        setRegistryName(LisBamPastoralEconomy.MODID, "transport_station");
        setUnlocalizedName(LisBamPastoralEconomy.MODID + ".transport_station");
        setHardness(3.0F);
        setResistance(12.0F);
        setCreativeTab(CreativeTabs.TRANSPORTATION);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public String getLocalizedName() {
        return I18n.translateToLocal(DISPLAY_NAME_KEY);
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileTransportStation();
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote && world.getTileEntity(pos) instanceof TileTransportStation) {
            TransportService.onStationPlaced(world, pos, (TileTransportStation) world.getTileEntity(pos), stack);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote && player instanceof EntityPlayerMP
                && world.getTileEntity(pos) instanceof TileTransportStation) {
            TransportService.openStationGui((EntityPlayerMP) player, world, pos,
                    (TileTransportStation) world.getTileEntity(pos));
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof TileTransportStation) {
            TransportService.unregisterBrokenStation(world, pos, (TileTransportStation) tile);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess blockAccess, BlockPos pos,
                         IBlockState state, int fortune) {
        ItemStack result = new ItemStack(ModItems.TRANSPORT_STATION_ITEM);
        TileEntity tile = blockAccess.getTileEntity(pos);
        if (tile instanceof TileTransportStation) {
            java.util.UUID owner = ((TileTransportStation) tile).getStarterOwner();
            if (owner != null) {
                StarterTransportItemData.markStarter(result, owner);
            }
        }
        drops.add(result);
    }
}
