package lisbam.pastoraleconomy.data.player;

/**
 * The single long-lived player data root for this mod. Normal gameplay code
 * reads and changes coins through CoinService rather than mutating this
 * capability directly.
 */
public interface IPlayerData {
    long getCoins();

    /**
     * Used only by the Forge player-clone lifecycle to preserve persistent
     * data when the player entity is replaced.
     */
    void copyFrom(IPlayerData source);
}
