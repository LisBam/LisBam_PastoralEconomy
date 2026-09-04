package lisbam.pastoraleconomy.gui;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.entity.EntityMerchant;
import lisbam.pastoraleconomy.tile.TileCrabTrap;
import lisbam.pastoraleconomy.tile.TileTransportStation;
import lisbam.pastoraleconomy.tile.TileVillageStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/**
 * Common-side GUI gateway. Client GuiScreen construction stays behind the
 * sided proxy, while this side owns the safe empty server Container.
 */
public final class ModGuiHandler implements IGuiHandler {
    @Override
    public Object getServerGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z) {
        GuiIds.validate(guiId);
        if (guiId == GuiIds.MERCHANT_TRADE) {
            if (world.getEntityByID(x) instanceof EntityMerchant) {
                return new ContainerMerchantTrade((EntityMerchant) world.getEntityByID(x));
            }
            return null;
        }
        if (guiId == GuiIds.CRAB_TRAP) {
            if (world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)) instanceof TileCrabTrap) {
                return new ContainerCrabTrap(player.inventory,
                        (TileCrabTrap) world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)));
            }
            return null;
        }
        if (guiId == GuiIds.TRANSPORT_STATION) {
            net.minecraft.util.math.BlockPos stationPos = new net.minecraft.util.math.BlockPos(x, y, z);
            if (world.getTileEntity(stationPos) instanceof TileTransportStation) {
                return new ContainerTransportStation((TileTransportStation) world.getTileEntity(stationPos));
            }
            if (world.getTileEntity(stationPos) instanceof TileVillageStation) {
                return new ContainerTransportStation((TileVillageStation) world.getTileEntity(stationPos));
            }
            return null;
        }
        return new ContainerMarketBook();
    }

    @Override
    public Object getClientGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z) {
        GuiIds.validate(guiId);
        return LisBamPastoralEconomy.proxy.getClientGuiElement(guiId, player, world, x, y, z);
    }
}
