package lisbam.pastoraleconomy.data.player;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Server-only access boundary for batch-14 player-owned transport records. */
public final class PlayerTransportDataService {
    private PlayerTransportDataService() {
    }

    public static boolean hasStarterTransportBeenGranted(EntityPlayerMP player) {
        return requireServerData(player).hasStarterTransportBeenGranted();
    }

    public static void markStarterTransportGranted(EntityPlayerMP player) {
        requireServerData(player).markStarterTransportGranted();
    }

    public static boolean hasEstablishedFirstSelfBuiltStation(EntityPlayerMP player) {
        return requireServerData(player).hasEstablishedFirstSelfBuiltStation();
    }

    public static boolean hasUsedFirstSelfBuiltStationFree(EntityPlayerMP player) {
        return requireServerData(player).hasUsedFirstSelfBuiltStationFree();
    }

    public static PlayerTransportNode getNode(EntityPlayerMP player, UUID stationId) {
        return requireServerData(player).getTransportNode(stationId);
    }

    public static List<PlayerTransportNode> getNodes(EntityPlayerMP player) {
        return Collections.unmodifiableList(requireServerData(player).getTransportNodes());
    }

    public static Collection<String> getAliasesExcept(EntityPlayerMP player, UUID stationId) {
        return requireServerData(player).getAliasesExcept(stationId);
    }

    public static boolean activateNode(EntityPlayerMP player, UUID stationId, String alias) {
        return requireServerData(player).activateTransportNode(stationId, alias);
    }

    public static boolean canActivateNode(EntityPlayerMP player, UUID stationId) {
        return requireServerData(player).canActivateTransportNode(stationId);
    }

    public static boolean deactivateNode(EntityPlayerMP player, UUID stationId) {
        return requireServerData(player).deactivateTransportNode(stationId);
    }

    public static boolean renameNode(EntityPlayerMP player, UUID stationId, String alias) {
        return requireServerData(player).renameTransportNode(stationId, alias);
    }

    public static int peekNextSelfBuiltStationSequence(EntityPlayerMP player) {
        return requireServerData(player).peekNextSelfBuiltStationSequence();
    }

    public static int takeNextSelfBuiltStationSequence(EntityPlayerMP player) {
        return requireServerData(player).takeNextSelfBuiltStationSequence();
    }

    /** Records permanent first-node history; freeUsed is true only for the actual free transaction. */
    public static void markFirstSelfBuiltStationEstablished(EntityPlayerMP player, boolean freeUsed) {
        requireServerData(player).markFirstSelfBuiltStationEstablished(freeUsed);
    }

    private static PlayerData requireServerData(EntityPlayerMP player) {
        if (player == null || player.world == null || player.world.isRemote) {
            throw new IllegalStateException("Transport player data is only valid on the logical server.");
        }
        IPlayerData data = PlayerDataCapability.get(player);
        if (!(data instanceof PlayerData)) {
            throw new IllegalStateException("Player transport capability is missing or incompatible.");
        }
        return (PlayerData) data;
    }
}
