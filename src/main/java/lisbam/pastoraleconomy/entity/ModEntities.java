package lisbam.pastoraleconomy.entity;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.tile.TileCrabTrap;
import lisbam.pastoraleconomy.tile.TileVillageStation;
import lisbam.pastoraleconomy.tile.TileTransportStation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Legacy 1.12.2 entity and tile registrations that do not use modern registry APIs. */
public final class ModEntities {
    private static boolean registered;

    private ModEntities() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        GameRegistry.registerTileEntity(TileVillageStation.class,
                new ResourceLocation(LisBamPastoralEconomy.MODID, "village_station"));
        GameRegistry.registerTileEntity(TileCrabTrap.class,
                new ResourceLocation(LisBamPastoralEconomy.MODID, "crab_trap"));
        GameRegistry.registerTileEntity(TileTransportStation.class,
                new ResourceLocation(LisBamPastoralEconomy.MODID, "transport_station"));
        EntityRegistry.registerModEntity(new ResourceLocation(LisBamPastoralEconomy.MODID, "merchant"),
                EntityMerchant.class, "merchant", 0, LisBamPastoralEconomy.INSTANCE, 64, 3, true);
    }
}
