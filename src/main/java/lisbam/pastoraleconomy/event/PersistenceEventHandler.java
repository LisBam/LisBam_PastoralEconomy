package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.data.player.CoinService;
import lisbam.pastoraleconomy.data.player.IPlayerData;
import lisbam.pastoraleconomy.data.player.PlayerDataCapability;
import lisbam.pastoraleconomy.data.player.PlayerDataProvider;
import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import lisbam.pastoraleconomy.market.MarketService;
import lisbam.pastoraleconomy.merchant.VillageService;
import lisbam.pastoraleconomy.transport.TransportService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Common-side lifecycle wiring for player data and shared world data. */
@Mod.EventBusSubscriber(modid = LisBamPastoralEconomy.MODID)
public final class PersistenceEventHandler {
    private PersistenceEventHandler() {
    }

    @SubscribeEvent
    public static void attachPlayerData(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(PlayerDataCapability.ID, new PlayerDataProvider());
        }
    }

    @SubscribeEvent
    public static void clonePlayerData(PlayerEvent.Clone event) {
        IPlayerData original = PlayerDataCapability.get(event.getOriginal());
        IPlayerData replacement = PlayerDataCapability.get(event.getEntityPlayer());
        if (original != null && replacement != null) {
            replacement.copyFrom(original);
        }
    }

    @SubscribeEvent
    public static void initializeWorld(WorldEvent.Load event) {
        World world = event.getWorld();
        if (!world.isRemote && world instanceof WorldServer && world.provider.getDimension() == 0) {
            PastoralWorldData.get(world).completeFirstInitialization((WorldServer) world);
            MarketService.tick((WorldServer) world);
            VillageService.tick((WorldServer) world, true);
        }
    }

    /** The market is global, so it advances once on the logical-server overworld only. */
    @SubscribeEvent
    public static void updateMarket(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote && event.world instanceof WorldServer
                && event.world.provider.getDimension() == 0) {
            MarketService.tick((WorldServer) event.world);
            VillageService.tick((WorldServer) event.world);
        }
    }

    @SubscribeEvent
    public static void syncOnLogin(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        syncPlayer(event.player);
    }

    @SubscribeEvent
    public static void syncOnRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        syncPlayer(event.player);
    }

    @SubscribeEvent
    public static void syncOnDimensionChange(
            net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        syncPlayer(event.player);
    }

    private static void syncPlayer(EntityPlayer player) {
        if (player instanceof EntityPlayerMP && !player.world.isRemote) {
            TransportService.grantStarterTransportIfNeeded((EntityPlayerMP) player);
            CoinService.syncToClient((EntityPlayerMP) player);
        }
    }
}
