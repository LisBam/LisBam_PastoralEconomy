package lisbam.pastoraleconomy.transport;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded registry embedded in the one shared PastoralWorldData root. */
public final class TransportWorldState {
    private static final String KEY_STATIONS = "stations";
    private static final int MAX_STATIONS = 8192;

    private final Map<UUID, TransportStationRecord> stations =
            new LinkedHashMap<UUID, TransportStationRecord>();

    public Collection<TransportStationRecord> getStations() {
        return Collections.unmodifiableCollection(stations.values());
    }

    public TransportStationRecord getStation(UUID stationId) {
        return stationId == null ? null : stations.get(stationId);
    }

    public TransportStationRecord getStationAt(int dimension, BlockPos position) {
        if (position == null) {
            return null;
        }
        for (TransportStationRecord record : stations.values()) {
            if (record.isAt(dimension, position)) {
                return record;
            }
        }
        return null;
    }

    public TransportStationRecord getVillageStation(UUID villageId) {
        if (villageId == null) {
            return null;
        }
        for (TransportStationRecord record : stations.values()) {
            if (record.getType() == TransportStationType.VILLAGE && villageId.equals(record.getVillageId())) {
                return record;
            }
        }
        return null;
    }

    public boolean putStation(TransportStationRecord record) {
        if (record == null || (stations.size() >= MAX_STATIONS && !stations.containsKey(record.getStationId()))) {
            return false;
        }
        TransportStationRecord old = stations.put(record.getStationId(), record);
        return old == null || !old.isAt(record.getDimension(), record.getPosition())
                || old.getType() != record.getType();
    }

    public boolean removeStation(UUID stationId, int expectedDimension, BlockPos expectedPosition) {
        TransportStationRecord record = getStation(stationId);
        if (record == null || !record.isAt(expectedDimension, expectedPosition)) {
            return false;
        }
        stations.remove(stationId);
        return true;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (TransportStationRecord record : stations.values()) {
            list.appendTag(record.writeToNBT());
        }
        tag.setTag(KEY_STATIONS, list);
        return tag;
    }

    public void readFromNBT(NBTTagCompound tag) {
        stations.clear();
        NBTTagList list = tag.getTagList(KEY_STATIONS, 10);
        for (int index = 0; index < list.tagCount() && stations.size() < MAX_STATIONS; index++) {
            TransportStationRecord record = TransportStationRecord.readFromNBT(list.getCompoundTagAt(index));
            if (record != null && !stations.containsKey(record.getStationId())
                    && getStationAt(record.getDimension(), record.getPosition()) == null) {
                stations.put(record.getStationId(), record);
            }
        }
    }
}
