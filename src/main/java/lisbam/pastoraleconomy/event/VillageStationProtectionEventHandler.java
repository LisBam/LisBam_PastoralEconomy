package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.block.ModBlocks;
import lisbam.pastoraleconomy.tile.TileVillageStation;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Prevents normal survival or creative break actions from duplicating village station blocks. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class VillageStationProtectionEventHandler {
    private VillageStationProtectionEventHandler() {
    }

    @SubscribeEvent
    public static void protectVillageStation(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote || event.getState().getBlock() != ModBlocks.VILLAGE_STATION) {
            return;
        }
        TileEntity tile = event.getWorld().getTileEntity(event.getPos());
        if (tile instanceof TileVillageStation && ((TileVillageStation) tile).isVillageStation()) {
            event.setCanceled(true);
        }
    }
}
