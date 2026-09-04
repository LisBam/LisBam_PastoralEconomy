package lisbam.pastoraleconomy.data.player;

import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.network.message.SyncCoinsMessage;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Server-authoritative coin domain API. No normal gameplay path receives a
 * writable balance or modifies player NBT directly.
 */
public final class CoinService {
    private CoinService() {
    }

    public static long getBalance(EntityPlayerMP player) {
        return requireServerData(player).getCoins();
    }

    public static boolean canAfford(EntityPlayerMP player, long amount) {
        return requireServerData(player).canAfford(amount);
    }

    public static boolean addCoins(EntityPlayerMP player, long amount) {
        PlayerData data = requireServerData(player);
        long previousBalance = data.getCoins();
        if (!data.addCoins(amount)) {
            return false;
        }
        if (data.getCoins() != previousBalance) {
            syncToClient(player);
        }
        return true;
    }

    public static boolean trySpend(EntityPlayerMP player, long amount) {
        PlayerData data = requireServerData(player);
        long previousBalance = data.getCoins();
        if (!data.trySpend(amount)) {
            return false;
        }
        if (data.getCoins() != previousBalance) {
            syncToClient(player);
        }
        return true;
    }

    public static void syncToClient(EntityPlayerMP player) {
        if (player.world.isRemote) {
            throw new IllegalStateException("Coins may only be synchronized from the logical server.");
        }
        ModNetwork.CHANNEL.sendTo(new SyncCoinsMessage(requireServerData(player).getCoins()), player);
    }

    private static PlayerData requireServerData(EntityPlayerMP player) {
        if (player == null || player.world == null || player.world.isRemote) {
            throw new IllegalStateException("Coin operations are only valid for a logical-server player.");
        }
        IPlayerData playerData = PlayerDataCapability.get(player);
        if (!(playerData instanceof PlayerData)) {
            throw new IllegalStateException("Player data capability is missing or incompatible.");
        }
        return (PlayerData) playerData;
    }
}
