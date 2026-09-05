package lisbam.pastoraleconomy.data.player;

import net.minecraft.item.ItemStack;

/**
 * The single long-lived player data root for this mod. Normal gameplay code
 * reads and changes coins through CoinService rather than mutating this
 * capability directly.
 */
public interface IPlayerData {
    long getCoins();

    /** The persistent one-slot shoulder equipment inventory. */
    ItemStack getShoulderStack();

    /** Only the shoulder inventory bridge may replace this stack. */
    void setShoulderStack(ItemStack stack);

    /**
     * Used only by the Forge player-clone lifecycle to preserve persistent
     * data when the player entity is replaced.
     */
    void copyFrom(IPlayerData source);
}
