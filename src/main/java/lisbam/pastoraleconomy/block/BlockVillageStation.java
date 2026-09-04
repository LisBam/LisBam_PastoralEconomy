package lisbam.pastoraleconomy.block;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.tile.TileVillageStation;
import lisbam.pastoraleconomy.transport.TransportService;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

import java.util.Random;

/** Server-created protected anchor for one legacy-village merchant roster. */
public final class BlockVillageStation extends Block {
    BlockVillageStation() {
        super(Material.WOOD);
        setRegistryName(LisBamPastoralEconomy.MODID, "village_station");
        setUnlocalizedName(LisBamPastoralEconomy.MODID + ".village_station");
        setHardness(-1.0F);
        setResistance(6000000.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileVillageStation();
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && player instanceof EntityPlayerMP && tile instanceof TileVillageStation
                && ((TileVillageStation) tile).isVillageStation()) {
            TransportService.openStationGui((EntityPlayerMP) player, world, pos, tile);
        }
        return true;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    /** Ordinary player break attempts cannot turn a village station into a collectible block. */
    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player,
                                   boolean willHarvest) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileVillageStation && ((TileVillageStation) tile).isVillageStation()) {
            return false;
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }
}
