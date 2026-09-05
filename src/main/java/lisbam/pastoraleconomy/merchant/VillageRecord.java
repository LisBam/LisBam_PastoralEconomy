package lisbam.pastoraleconomy.merchant;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Stable record for one recognised legacy Overworld village settlement. */
public final class VillageRecord {
    private static final String KEY_ID = "villageId";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_Z = "z";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_VILLAGERS = "villagerCount";
    private static final String KEY_STATION = "stationId";
    private static final String KEY_ROSTER = "merchantRoster";
    private static final String KEY_MERCHANT_ID = "merchantId";

    private final UUID villageId;
    private int centerX;
    private int centerY;
    private int centerZ;
    private boolean active;
    private int villagerCount;
    private UUID stationId;
    private final List<UUID> merchantRoster = new ArrayList<UUID>();

    public VillageRecord(UUID villageId, int centerX, int centerY, int centerZ) {
        if (villageId == null) {
            throw new IllegalArgumentException("Village ID is required.");
        }
        this.villageId = villageId;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
    }

    public UUID getVillageId() { return villageId; }
    public int getCenterX() { return centerX; }
    public int getCenterY() { return centerY; }
    public int getCenterZ() { return centerZ; }
    public BlockPos getCenter() { return new BlockPos(centerX, centerY, centerZ); }
    public boolean isActive() { return active; }
    public int getVillagerCount() { return villagerCount; }
    public UUID getStationId() { return stationId; }
    public List<UUID> getMerchantRoster() { return Collections.unmodifiableList(merchantRoster); }

    public void updateObservation(int x, int y, int z, int observedVillagers, boolean observedActive) {
        centerX = x;
        centerY = y;
        centerZ = z;
        villagerCount = Math.max(0, observedVillagers);
        active = observedActive;
    }

    public void markInactive() {
        active = false;
        villagerCount = 0;
    }

    public void setStationId(UUID value) { stationId = value; }

    public void addMerchant(UUID merchantId) {
        if (merchantId != null && !merchantRoster.contains(merchantId)) {
            merchantRoster.add(merchantId);
        }
    }

    public void removeMerchant(UUID merchantId) {
        merchantRoster.remove(merchantId);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId(KEY_ID, villageId);
        tag.setInteger(KEY_X, centerX);
        tag.setInteger(KEY_Y, centerY);
        tag.setInteger(KEY_Z, centerZ);
        tag.setBoolean(KEY_ACTIVE, active);
        tag.setInteger(KEY_VILLAGERS, villagerCount);
        if (stationId != null) {
            tag.setUniqueId(KEY_STATION, stationId);
        }
        NBTTagList roster = new NBTTagList();
        for (UUID merchantId : merchantRoster) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setUniqueId(KEY_MERCHANT_ID, merchantId);
            roster.appendTag(entry);
        }
        tag.setTag(KEY_ROSTER, roster);
        return tag;
    }

    public static VillageRecord readFromNBT(NBTTagCompound tag) {
        if (!tag.hasUniqueId(KEY_ID)) {
            return null;
        }
        VillageRecord record = new VillageRecord(tag.getUniqueId(KEY_ID), tag.getInteger(KEY_X), tag.getInteger(KEY_Y),
                tag.getInteger(KEY_Z));
        record.active = tag.getBoolean(KEY_ACTIVE);
        record.villagerCount = Math.max(0, tag.getInteger(KEY_VILLAGERS));
        record.stationId = tag.hasUniqueId(KEY_STATION) ? tag.getUniqueId(KEY_STATION) : null;
        NBTTagList roster = tag.getTagList(KEY_ROSTER, 10);
        for (int index = 0; index < roster.tagCount(); index++) {
            NBTTagCompound entry = roster.getCompoundTagAt(index);
            if (entry.hasUniqueId(KEY_MERCHANT_ID)) {
                record.addMerchant(entry.getUniqueId(KEY_MERCHANT_ID));
            }
        }
        return record;
    }
}
