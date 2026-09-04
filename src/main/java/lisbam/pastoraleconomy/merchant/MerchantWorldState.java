package lisbam.pastoraleconomy.merchant;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Merchant, village, and station world state embedded in the single PastoralWorldData root. */
public final class MerchantWorldState {
    private static final String KEY_VILLAGES = "villages";
    private static final String KEY_STATIONS = "stations";
    private static final String KEY_MERCHANTS = "merchants";
    private static final int MAX_RECORDS_PER_KIND = 8192;

    private final Map<UUID, VillageRecord> villages = new LinkedHashMap<UUID, VillageRecord>();
    private final Map<UUID, StationRecord> stations = new LinkedHashMap<UUID, StationRecord>();
    private final Map<UUID, MerchantRecord> merchants = new LinkedHashMap<UUID, MerchantRecord>();

    public Collection<VillageRecord> getVillages() { return Collections.unmodifiableCollection(villages.values()); }
    public Collection<StationRecord> getStations() { return Collections.unmodifiableCollection(stations.values()); }
    public Collection<MerchantRecord> getMerchants() { return Collections.unmodifiableCollection(merchants.values()); }
    public VillageRecord getVillage(UUID id) { return villages.get(id); }
    public StationRecord getStation(UUID id) { return stations.get(id); }
    public StationRecord getStationByVillage(UUID villageId) {
        if (villageId == null) {
            return null;
        }
        for (StationRecord record : stations.values()) {
            if (villageId.equals(record.getVillageId()) && record.getRole() == StationRole.VILLAGE) {
                return record;
            }
        }
        return null;
    }
    public MerchantRecord getMerchant(UUID id) { return merchants.get(id); }

    public void putVillage(VillageRecord record) { villages.put(record.getVillageId(), record); }
    public void putStation(StationRecord record) { stations.put(record.getStationId(), record); }
    public void putMerchant(MerchantRecord record) { merchants.put(record.getMerchantId(), record); }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(KEY_VILLAGES, writeVillages());
        tag.setTag(KEY_STATIONS, writeStations());
        tag.setTag(KEY_MERCHANTS, writeMerchants());
        return tag;
    }

    public void readFromNBT(NBTTagCompound tag) {
        villages.clear();
        stations.clear();
        merchants.clear();
        readVillages(tag.getTagList(KEY_VILLAGES, 10));
        readStations(tag.getTagList(KEY_STATIONS, 10));
        readMerchants(tag.getTagList(KEY_MERCHANTS, 10));
    }

    private NBTTagList writeVillages() {
        NBTTagList list = new NBTTagList();
        for (VillageRecord record : villages.values()) { list.appendTag(record.writeToNBT()); }
        return list;
    }

    private NBTTagList writeStations() {
        NBTTagList list = new NBTTagList();
        for (StationRecord record : stations.values()) { list.appendTag(record.writeToNBT()); }
        return list;
    }

    private NBTTagList writeMerchants() {
        NBTTagList list = new NBTTagList();
        for (MerchantRecord record : merchants.values()) { list.appendTag(record.writeToNBT()); }
        return list;
    }

    private void readVillages(NBTTagList list) {
        for (int index = 0; index < list.tagCount() && villages.size() < MAX_RECORDS_PER_KIND; index++) {
            VillageRecord record = VillageRecord.readFromNBT(list.getCompoundTagAt(index));
            if (record != null && !villages.containsKey(record.getVillageId())) { putVillage(record); }
        }
    }

    private void readStations(NBTTagList list) {
        for (int index = 0; index < list.tagCount() && stations.size() < MAX_RECORDS_PER_KIND; index++) {
            StationRecord record = StationRecord.readFromNBT(list.getCompoundTagAt(index));
            if (record != null && !stations.containsKey(record.getStationId())) { putStation(record); }
        }
    }

    private void readMerchants(NBTTagList list) {
        for (int index = 0; index < list.tagCount() && merchants.size() < MAX_RECORDS_PER_KIND; index++) {
            MerchantRecord record = MerchantRecord.readFromNBT(list.getCompoundTagAt(index));
            if (record != null && !merchants.containsKey(record.getMerchantId())) { putMerchant(record); }
        }
    }
}
