package lisbam.pastoraleconomy.merchant;

import net.minecraft.nbt.NBTTagCompound;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Stable logical merchant identity; entity death never discards its offers or stock. */
public final class MerchantRecord {
    private static final String KEY_ID = "merchantId";
    private static final String KEY_VILLAGE = "villageId";
    private static final String KEY_STATION = "stationId";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_DAILY = "dailyOffer";
    private static final String KEY_ROTATIONS = "rotations";

    private final UUID merchantId;
    private final UUID villageId;
    private final UUID stationId;
    private boolean active = true;
    private DailyOfferState dailyOfferState;
    private final Map<TradePool, OfferRotationState> rotations = new EnumMap<TradePool, OfferRotationState>(TradePool.class);

    public MerchantRecord(UUID merchantId, UUID villageId, UUID stationId) {
        if (merchantId == null || villageId == null || stationId == null) {
            throw new IllegalArgumentException("Merchant identity is incomplete.");
        }
        this.merchantId = merchantId;
        this.villageId = villageId;
        this.stationId = stationId;
    }

    public UUID getMerchantId() { return merchantId; }
    public UUID getVillageId() { return villageId; }
    public UUID getStationId() { return stationId; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
    public DailyOfferState getDailyOfferState() { return dailyOfferState; }
    public void setDailyOfferState(DailyOfferState value) { dailyOfferState = value; }

    public OfferRotationState getRotation(TradePool pool) {
        OfferRotationState state = rotations.get(pool);
        if (state == null) {
            state = new OfferRotationState();
            rotations.put(pool, state);
        }
        return state;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId(KEY_ID, merchantId);
        tag.setUniqueId(KEY_VILLAGE, villageId);
        tag.setUniqueId(KEY_STATION, stationId);
        tag.setBoolean(KEY_ACTIVE, active);
        if (dailyOfferState != null) {
            tag.setTag(KEY_DAILY, dailyOfferState.writeToNBT());
        }
        NBTTagCompound rotationTags = new NBTTagCompound();
        for (Map.Entry<TradePool, OfferRotationState> entry : rotations.entrySet()) {
            rotationTags.setTag(entry.getKey().name(), entry.getValue().writeToNBT());
        }
        tag.setTag(KEY_ROTATIONS, rotationTags);
        return tag;
    }

    public static MerchantRecord readFromNBT(NBTTagCompound tag) {
        if (!tag.hasUniqueId(KEY_ID) || !tag.hasUniqueId(KEY_VILLAGE) || !tag.hasUniqueId(KEY_STATION)) {
            return null;
        }
        MerchantRecord record = new MerchantRecord(tag.getUniqueId(KEY_ID), tag.getUniqueId(KEY_VILLAGE),
                tag.getUniqueId(KEY_STATION));
        record.active = tag.getBoolean(KEY_ACTIVE);
        if (tag.hasKey(KEY_DAILY, 10)) {
            record.dailyOfferState = DailyOfferState.readFromNBT(tag.getCompoundTag(KEY_DAILY));
        }
        NBTTagCompound rotationTags = tag.getCompoundTag(KEY_ROTATIONS);
        for (TradePool pool : TradePool.values()) {
            if (rotationTags.hasKey(pool.name(), 10)) {
                record.rotations.put(pool, OfferRotationState.readFromNBT(rotationTags.getCompoundTag(pool.name())));
            }
        }
        return record;
    }
}
