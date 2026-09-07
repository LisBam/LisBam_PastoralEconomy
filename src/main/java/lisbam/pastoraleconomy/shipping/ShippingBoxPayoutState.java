package lisbam.pastoraleconomy.shipping;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded world-owned pending shipping earnings and once-per-day dispatch marker. */
public final class ShippingBoxPayoutState {
    private static final String KEY_LAST_DISPATCH_DAY = "lastDispatchDay";
    private static final String KEY_PENDING = "pending";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_AMOUNT = "amount";
    private static final int MAX_PENDING_PLAYERS = 8192;

    private long lastDispatchDay = -1L;
    private final Map<UUID, Long> pendingByOwner = new LinkedHashMap<UUID, Long>();

    /** A day is claimed only once; administrator time rollback never creates a duplicate dispatch. */
    public boolean tryBeginDispatch(long worldDay) {
        if (worldDay < 0L || worldDay <= lastDispatchDay) {
            return false;
        }
        lastDispatchDay = worldDay;
        return true;
    }

    public long getLastDispatchDay() {
        return lastDispatchDay;
    }

    public long getPending(UUID owner) {
        Long value = owner == null ? null : pendingByOwner.get(owner);
        return value == null ? 0L : value.longValue();
    }

    public Map<UUID, Long> getPendingByOwner() {
        return Collections.unmodifiableMap(new LinkedHashMap<UUID, Long>(pendingByOwner));
    }

    /** Validates the entire planned allocation before any inventory is consumed. */
    public boolean canCredit(Map<UUID, Long> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            return false;
        }
        int newOwners = 0;
        for (Map.Entry<UUID, Long> entry : allocations.entrySet()) {
            UUID owner = entry.getKey();
            Long amount = entry.getValue();
            if (owner == null || amount == null || amount.longValue() < 0L) {
                return false;
            }
            if (!pendingByOwner.containsKey(owner)) {
                newOwners++;
            }
            long existing = getPending(owner);
            if (amount.longValue() > Long.MAX_VALUE - existing) {
                return false;
            }
        }
        return pendingByOwner.size() <= MAX_PENDING_PLAYERS - newOwners;
    }

    public boolean credit(Map<UUID, Long> allocations) {
        if (!canCredit(allocations)) {
            return false;
        }
        for (Map.Entry<UUID, Long> entry : allocations.entrySet()) {
            long amount = entry.getValue().longValue();
            if (amount > 0L) {
                pendingByOwner.put(entry.getKey(), Long.valueOf(getPending(entry.getKey()) + amount));
            }
        }
        return true;
    }

    /** Removes at most the requested amount after a successful CoinService credit. */
    public boolean claim(UUID owner, long amount) {
        long pending = getPending(owner);
        if (owner == null || amount <= 0L || amount > pending) {
            return false;
        }
        long remaining = pending - amount;
        if (remaining == 0L) {
            pendingByOwner.remove(owner);
        } else {
            pendingByOwner.put(owner, Long.valueOf(remaining));
        }
        return true;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong(KEY_LAST_DISPATCH_DAY, lastDispatchDay);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, Long> entry : pendingByOwner.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().longValue() <= 0L) {
                continue;
            }
            NBTTagCompound pending = new NBTTagCompound();
            pending.setUniqueId(KEY_OWNER, entry.getKey());
            pending.setLong(KEY_AMOUNT, entry.getValue().longValue());
            list.appendTag(pending);
        }
        tag.setTag(KEY_PENDING, list);
        return tag;
    }

    public void readFromNBT(NBTTagCompound tag) {
        pendingByOwner.clear();
        lastDispatchDay = tag == null ? -1L : Math.max(-1L, tag.getLong(KEY_LAST_DISPATCH_DAY));
        if (tag == null) {
            return;
        }
        NBTTagList list = tag.getTagList(KEY_PENDING, 10);
        for (int index = 0; index < list.tagCount() && pendingByOwner.size() < MAX_PENDING_PLAYERS; index++) {
            NBTTagCompound entry = list.getCompoundTagAt(index);
            if (!entry.hasUniqueId(KEY_OWNER)) {
                continue;
            }
            UUID owner = entry.getUniqueId(KEY_OWNER);
            long amount = entry.getLong(KEY_AMOUNT);
            if (amount <= 0L || pendingByOwner.containsKey(owner)) {
                continue;
            }
            pendingByOwner.put(owner, Long.valueOf(amount));
        }
    }
}
