package lisbam.pastoraleconomy.data.player;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.item.ItemStack;
import lisbam.pastoraleconomy.equipment.ShoulderEquipmentService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Package-private mutable implementation; CoinService is the only gameplay mutator. */
final class PlayerData implements IPlayerData {
    static final int DATA_VERSION = 3;
    private static final int MAX_TRANSPORT_NODES = 2048;

    private static final String KEY_DATA_VERSION = "dataVersion";
    private static final String KEY_COINS = "coins";
    private static final String KEY_SHOULDER = "shoulder";
    private static final String KEY_TRANSPORT = "transport";
    private static final String KEY_STARTER_TRANSPORT_GRANTED = "starterTransportGranted";
    private static final String KEY_FIRST_SELF_BUILT_STATION_ESTABLISHED = "firstSelfBuiltStationEstablished";
    private static final String KEY_FREE_STATION_USED = "firstSelfBuiltStationFreeUsed";
    private static final String KEY_TRANSPORT_NODES = "nodes";
    private static final String KEY_NEXT_SELF_BUILT_STATION_SEQUENCE = "nextSelfBuiltStationSequence";
    private static final String KEY_UNLOCKED_NODES = "unlockedNodes";
    private static final String KEY_UNLOCKED_ROUTES = "unlockedRoutes";
    private static final String KEY_DESTINATION_ALIASES = "destinationAliases";
    private static final String KEY_ALIAS_ID = "id";
    private static final String KEY_ALIAS_VALUE = "alias";

    private int dataVersion = DATA_VERSION;
    private long coins;
    private ItemStack shoulderStack = ItemStack.EMPTY;
    private boolean starterTransportGranted;
    private boolean firstSelfBuiltStationEstablished;
    private boolean firstSelfBuiltStationFreeUsed;
    private int nextSelfBuiltStationSequence = 1;
    private final Map<UUID, PlayerTransportNode> transportNodes =
            new LinkedHashMap<UUID, PlayerTransportNode>();
    // These v1 placeholders are retained during migration only. They were not
    // exposed as gameplay state before batch 14.
    private final Set<String> unlockedNodes = new LinkedHashSet<String>();
    private final Set<String> unlockedRoutes = new LinkedHashSet<String>();
    private final Map<String, String> destinationAliases = new LinkedHashMap<String, String>();

    @Override
    public long getCoins() {
        return coins;
    }

    @Override
    public ItemStack getShoulderStack() {
        return shoulderStack;
    }

    @Override
    public void setShoulderStack(ItemStack stack) {
        if (!ShoulderEquipmentService.isValidShoulderStack(stack)) {
            shoulderStack = ItemStack.EMPTY;
            return;
        }
        ItemStack normalized = stack.copy();
        normalized.setCount(1);
        shoulderStack = normalized;
    }

    boolean canAfford(long amount) {
        return amount >= 0L && amount <= coins;
    }

    boolean addCoins(long amount) {
        if (amount < 0L || amount > Long.MAX_VALUE - coins) {
            return false;
        }
        if (amount > 0L) {
            coins += amount;
        }
        return true;
    }

    boolean trySpend(long amount) {
        if (!canAfford(amount)) {
            return false;
        }
        if (amount > 0L) {
            coins -= amount;
        }
        return true;
    }

    @Override
    public void copyFrom(IPlayerData source) {
        if (!(source instanceof PlayerData)) {
            throw new IllegalArgumentException("Cannot copy an incompatible player data implementation.");
        }

        PlayerData sourceData = (PlayerData) source;
        coins = sourceData.coins;
        shoulderStack = sourceData.shoulderStack.isEmpty() ? ItemStack.EMPTY : sourceData.shoulderStack.copy();
        starterTransportGranted = sourceData.starterTransportGranted;
        firstSelfBuiltStationEstablished = sourceData.firstSelfBuiltStationEstablished;
        firstSelfBuiltStationFreeUsed = sourceData.firstSelfBuiltStationFreeUsed;
        nextSelfBuiltStationSequence = sourceData.nextSelfBuiltStationSequence;
        transportNodes.clear();
        transportNodes.putAll(sourceData.transportNodes);
        unlockedNodes.clear();
        unlockedNodes.addAll(sourceData.unlockedNodes);
        unlockedRoutes.clear();
        unlockedRoutes.addAll(sourceData.unlockedRoutes);
        destinationAliases.clear();
        destinationAliases.putAll(sourceData.destinationAliases);
    }

    NBTTagCompound writeToNBT() {
        NBTTagCompound root = new NBTTagCompound();
        dataVersion = DATA_VERSION;
        root.setInteger(KEY_DATA_VERSION, dataVersion);
        root.setLong(KEY_COINS, coins);
        if (!shoulderStack.isEmpty()) {
            root.setTag(KEY_SHOULDER, shoulderStack.writeToNBT(new NBTTagCompound()));
        }

        NBTTagCompound transport = new NBTTagCompound();
        transport.setBoolean(KEY_STARTER_TRANSPORT_GRANTED, starterTransportGranted);
        transport.setBoolean(KEY_FIRST_SELF_BUILT_STATION_ESTABLISHED, firstSelfBuiltStationEstablished);
        transport.setBoolean(KEY_FREE_STATION_USED, firstSelfBuiltStationFreeUsed);
        transport.setInteger(KEY_NEXT_SELF_BUILT_STATION_SEQUENCE, nextSelfBuiltStationSequence);
        transport.setTag(KEY_TRANSPORT_NODES, writeTransportNodes());
        transport.setTag(KEY_UNLOCKED_NODES, writeStringSet(unlockedNodes));
        transport.setTag(KEY_UNLOCKED_ROUTES, writeStringSet(unlockedRoutes));
        transport.setTag(KEY_DESTINATION_ALIASES, writeAliases(destinationAliases));
        root.setTag(KEY_TRANSPORT, transport);
        return root;
    }

    void readFromNBT(NBTTagCompound root) {
        resetToDefaults();
        // Missing or legacy versions are upgraded through safe defaults.
        dataVersion = root.hasKey(KEY_DATA_VERSION) ? root.getInteger(KEY_DATA_VERSION) : 0;
        if (dataVersion < DATA_VERSION) {
            dataVersion = DATA_VERSION;
        }

        if (root.hasKey(KEY_COINS)) {
            long loadedCoins = root.getLong(KEY_COINS);
            coins = loadedCoins < 0L ? 0L : loadedCoins;
        }

        if (root.hasKey(KEY_SHOULDER, 10)) {
            setShoulderStack(new ItemStack(root.getCompoundTag(KEY_SHOULDER)));
        }

        if (!root.hasKey(KEY_TRANSPORT)) {
            return;
        }

        NBTTagCompound transport = root.getCompoundTag(KEY_TRANSPORT);
        starterTransportGranted = transport.getBoolean(KEY_STARTER_TRANSPORT_GRANTED);
        firstSelfBuiltStationEstablished = transport.getBoolean(KEY_FIRST_SELF_BUILT_STATION_ESTABLISHED);
        firstSelfBuiltStationFreeUsed = transport.getBoolean(KEY_FREE_STATION_USED);
        nextSelfBuiltStationSequence = Math.max(1, transport.getInteger(KEY_NEXT_SELF_BUILT_STATION_SEQUENCE));
        readTransportNodes(transport.getTagList(KEY_TRANSPORT_NODES, 10));
        readStringSet(transport.getTagList(KEY_UNLOCKED_NODES, 8), unlockedNodes);
        readStringSet(transport.getTagList(KEY_UNLOCKED_ROUTES, 8), unlockedRoutes);
        readAliases(transport.getTagList(KEY_DESTINATION_ALIASES, 10), destinationAliases);
        migrateV1TransportIfNeeded();
    }

    private void resetToDefaults() {
        dataVersion = DATA_VERSION;
        coins = 0L;
        shoulderStack = ItemStack.EMPTY;
        starterTransportGranted = false;
        firstSelfBuiltStationEstablished = false;
        firstSelfBuiltStationFreeUsed = false;
        nextSelfBuiltStationSequence = 1;
        transportNodes.clear();
        unlockedNodes.clear();
        unlockedRoutes.clear();
        destinationAliases.clear();
    }

    private static NBTTagList writeStringSet(Set<String> values) {
        NBTTagList list = new NBTTagList();
        for (String value : values) {
            list.appendTag(new NBTTagString(value));
        }
        return list;
    }

    boolean hasStarterTransportBeenGranted() {
        return starterTransportGranted;
    }

    void markStarterTransportGranted() {
        starterTransportGranted = true;
    }

    boolean hasEstablishedFirstSelfBuiltStation() {
        return firstSelfBuiltStationEstablished;
    }

    boolean hasUsedFirstSelfBuiltStationFree() {
        return firstSelfBuiltStationFreeUsed;
    }

    PlayerTransportNode getTransportNode(UUID stationId) {
        return stationId == null ? null : transportNodes.get(stationId);
    }

    List<PlayerTransportNode> getTransportNodes() {
        return new ArrayList<PlayerTransportNode>(transportNodes.values());
    }

    Collection<String> getAliasesExcept(UUID stationId) {
        List<String> aliases = new ArrayList<String>();
        for (PlayerTransportNode node : transportNodes.values()) {
            if (!node.getStationId().equals(stationId)) {
                aliases.add(node.getAlias());
            }
        }
        return aliases;
    }

    boolean activateTransportNode(UUID stationId, String alias) {
        if (stationId == null || alias == null) {
            return false;
        }
        PlayerTransportNode existing = transportNodes.get(stationId);
        if (existing == null && transportNodes.size() >= MAX_TRANSPORT_NODES) {
            return false;
        }
        transportNodes.put(stationId, new PlayerTransportNode(stationId, true,
                existing == null ? alias : existing.getAlias()));
        return true;
    }

    boolean canActivateTransportNode(UUID stationId) {
        return stationId != null && (transportNodes.containsKey(stationId) || transportNodes.size() < MAX_TRANSPORT_NODES);
    }

    boolean removeTransportNode(UUID stationId) {
        return stationId != null && transportNodes.remove(stationId) != null;
    }

    boolean renameTransportNode(UUID stationId, String alias) {
        PlayerTransportNode existing = getTransportNode(stationId);
        if (existing == null || alias == null) {
            return false;
        }
        transportNodes.put(stationId, new PlayerTransportNode(stationId, existing.isActive(), alias));
        return true;
    }

    int peekNextSelfBuiltStationSequence() {
        return nextSelfBuiltStationSequence;
    }

    int takeNextSelfBuiltStationSequence() {
        int sequence = nextSelfBuiltStationSequence;
        if (nextSelfBuiltStationSequence < Integer.MAX_VALUE) {
            nextSelfBuiltStationSequence++;
        }
        return sequence;
    }

    void markFirstSelfBuiltStationEstablished(boolean freeUsed) {
        firstSelfBuiltStationEstablished = true;
        if (freeUsed) {
            firstSelfBuiltStationFreeUsed = true;
        }
    }

    private NBTTagList writeTransportNodes() {
        NBTTagList list = new NBTTagList();
        for (PlayerTransportNode node : transportNodes.values()) {
            list.appendTag(node.writeToNBT());
        }
        return list;
    }

    private void readTransportNodes(NBTTagList list) {
        for (int index = 0; index < list.tagCount() && transportNodes.size() < MAX_TRANSPORT_NODES; index++) {
            PlayerTransportNode node = PlayerTransportNode.readFromNBT(list.getCompoundTagAt(index));
            // Earlier builds retained inactive nodes after removal. They are no
            // longer part of the player network and are discarded on load.
            if (node != null && node.isActive() && !transportNodes.containsKey(node.getStationId())) {
                transportNodes.put(node.getStationId(), node);
            }
        }
    }

    private void migrateV1TransportIfNeeded() {
        if (!transportNodes.isEmpty()) {
            return;
        }
        for (String legacyId : unlockedNodes) {
            try {
                UUID stationId = UUID.fromString(legacyId);
                String alias = destinationAliases.get(legacyId);
                if (alias == null || alias.isEmpty()) {
                    alias = "自建站点 #" + String.format(java.util.Locale.ROOT, "%03d", nextSelfBuiltStationSequence++);
                }
                if (transportNodes.size() < MAX_TRANSPORT_NODES) {
                    transportNodes.put(stationId, new PlayerTransportNode(stationId, true, alias));
                }
            } catch (IllegalArgumentException ignored) {
                // v1 fields were inactive placeholders; malformed values are not gameplay state.
            }
        }
        if (!transportNodes.isEmpty()) {
            firstSelfBuiltStationEstablished = true;
            firstSelfBuiltStationFreeUsed = true;
        }
    }

    private static NBTTagList writeAliases(Map<String, String> aliases) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            NBTTagCompound alias = new NBTTagCompound();
            alias.setString(KEY_ALIAS_ID, entry.getKey());
            alias.setString(KEY_ALIAS_VALUE, entry.getValue());
            list.appendTag(alias);
        }
        return list;
    }

    private static void readStringSet(NBTTagList list, Set<String> output) {
        for (int index = 0; index < list.tagCount(); index++) {
            String value = list.getStringTagAt(index);
            if (!value.isEmpty()) {
                output.add(value);
            }
        }
    }

    private static void readAliases(NBTTagList list, Map<String, String> output) {
        for (int index = 0; index < list.tagCount(); index++) {
            NBTTagCompound alias = list.getCompoundTagAt(index);
            String id = alias.getString(KEY_ALIAS_ID);
            if (!id.isEmpty()) {
                output.put(id, alias.getString(KEY_ALIAS_VALUE));
            }
        }
    }
}
