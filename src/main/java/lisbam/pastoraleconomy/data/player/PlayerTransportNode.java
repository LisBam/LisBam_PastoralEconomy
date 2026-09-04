package lisbam.pastoraleconomy.data.player;

import lisbam.pastoraleconomy.transport.TransportNameRules;
import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

/** Immutable per-player association with one world-owned station UUID. */
public final class PlayerTransportNode {
    private static final String KEY_STATION_ID = "stationId";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_ALIAS = "alias";

    private final UUID stationId;
    private final boolean active;
    private final String alias;

    PlayerTransportNode(UUID stationId, boolean active, String alias) {
        if (stationId == null || !TransportNameRules.isValidAlias(alias)) {
            throw new IllegalArgumentException("Player transport node is incomplete.");
        }
        this.stationId = stationId;
        this.active = active;
        this.alias = alias;
    }

    public UUID getStationId() {
        return stationId;
    }

    public boolean isActive() {
        return active;
    }

    public String getAlias() {
        return alias;
    }

    NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId(KEY_STATION_ID, stationId);
        tag.setBoolean(KEY_ACTIVE, active);
        tag.setString(KEY_ALIAS, alias);
        return tag;
    }

    static PlayerTransportNode readFromNBT(NBTTagCompound tag) {
        if (!tag.hasUniqueId(KEY_STATION_ID)) {
            return null;
        }
        String alias = tag.getString(KEY_ALIAS);
        return TransportNameRules.isValidAlias(alias)
                ? new PlayerTransportNode(tag.getUniqueId(KEY_STATION_ID), tag.getBoolean(KEY_ACTIVE), alias)
                : null;
    }
}
