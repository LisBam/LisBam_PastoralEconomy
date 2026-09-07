package lisbam.pastoraleconomy.event;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.data.player.CoinService;
import lisbam.pastoraleconomy.data.player.IPlayerData;
import lisbam.pastoraleconomy.data.player.PlayerDataCapability;
import lisbam.pastoraleconomy.data.player.PlayerDataProvider;
import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import lisbam.pastoraleconomy.chunkloader.ChunkLoaderService;
import lisbam.pastoraleconomy.equipment.ShoulderEquipmentService;
import lisbam.pastoraleconomy.merchant.VillageService;
import lisbam.pastoraleconomy.merchant.TradeVoucherStorageService;
import lisbam.pastoraleconomy.transport.TransportService;
import net.minecraft.entity.Entity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerChest;
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
            if (event.isWasDeath() && EnchantmentHelper.hasVanishingCurse(replacement.getShoulderStack())) {
                replacement.setShoulderStack(net.minecraft.item.ItemStack.EMPTY);
            }
        }
    }

    @SubscribeEvent
    public static void initializeWorld(WorldEvent.Load event) {
        World world = event.getWorld();
        if (!world.isRemote && world instanceof WorldServer && world.provider.getDimension() == 0) {
            PastoralWorldData.get(world).completeFirstInitialization((WorldServer) world);
        }
    }

    @SubscribeEvent
    public static void forgetVoucherChestTickets(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) {
            TradeVoucherStorageService.forgetWorld(event.getWorld());
            ChunkLoaderService.forgetWorld(event.getWorld());
        }
    }

    /** Closing a vanilla chest is the reliable server-side point after a voucher move has committed. */
    @SubscribeEvent
    public static void observeVoucherChestContents(
            net.minecraftforge.event.entity.player.PlayerContainerEvent.Close event
    ) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP && !event.getEntityPlayer().world.isRemote
                && event.getContainer() instanceof ContainerChest) {
            TradeVoucherStorageService.observeLoadedVoucherChests((WorldServer) event.getEntityPlayer().world);
        }
    }

    /** Merchant reconciliation is global and runs on the logical-server overworld only. */
    @SubscribeEvent
    public static void updateMarket(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote && event.world instanceof WorldServer
                && event.world.provider.getDimension() == 0) {
            VillageService.tick((WorldServer) event.world);
            if (event.world.getTotalWorldTime() % 20L == 0L) {
                TradeVoucherStorageService.reconcile((WorldServer) event.world);
            }
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

    @SubscribeEvent
    public static void syncTrackedShoulder(PlayerEvent.StartTracking event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP && event.getTarget() instanceof EntityPlayer
                && !event.getEntityPlayer().world.isRemote) {
            ShoulderEquipmentService.syncToPlayer(
                    (EntityPlayerMP) event.getEntityPlayer(), (EntityPlayer) event.getTarget());
        }
    }

    /** Detects in-place Elytra damage/breakage, which does not replace the Capability stack reference. */
    @SubscribeEvent
    public static void syncChangedShoulder(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof EntityPlayerMP
                && !event.player.world.isRemote) {
            ShoulderEquipmentService.syncIfChanged((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public static void forgetShoulderSync(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        ShoulderEquipmentService.forgetSyncedState(event.player);
    }

    private static void syncPlayer(EntityPlayer player) {
        if (player instanceof EntityPlayerMP && !player.world.isRemote) {
            ShoulderEquipmentService.migrateLegacyChestElytra((EntityPlayerMP) player);
            ShoulderEquipmentService.syncNow((EntityPlayerMP) player);
            TransportService.grantStarterTransportIfNeeded((EntityPlayerMP) player);
            CoinService.syncToClient((EntityPlayerMP) player);
        }
    }
}
