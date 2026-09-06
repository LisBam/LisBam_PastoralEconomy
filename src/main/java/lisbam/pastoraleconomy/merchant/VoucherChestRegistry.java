package lisbam.pastoraleconomy.merchant;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded world-owned index of chest blocks that currently contain a bound voucher. */
public final class VoucherChestRegistry {
    private static final String KEY_CHESTS = "voucherChests";
    private static final String KEY_DIMENSION = "dimension";
    private static final String KEY_POSITION = "position";
    private static final int MAX_CHESTS = 8192;

    private final Map<Location, Location> locations = new LinkedHashMap<Location, Location>();

    public boolean add(int dimension, BlockPos position) {
        if (position == null) {
            return false;
        }
        Location location = new Location(dimension, position.toLong());
        if (locations.containsKey(location) || locations.size() >= MAX_CHESTS) {
            return false;
        }
        locations.put(location, location);
        return true;
    }

    public boolean remove(Location location) {
        return location != null && locations.remove(location) != null;
    }

    public Collection<Location> getLocations() {
        List<Location> result = new ArrayList<Location>(locations.values());
        Collections.sort(result, Location.ORDER);
        return Collections.unmodifiableList(result);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (Location location : getLocations()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger(KEY_DIMENSION, location.dimension);
            entry.setLong(KEY_POSITION, location.packedPosition);
            list.appendTag(entry);
        }
        tag.setTag(KEY_CHESTS, list);
        return tag;
    }

    public void readFromNBT(NBTTagCompound tag) {
        locations.clear();
        NBTTagList list = tag.getTagList(KEY_CHESTS, 10);
        for (int index = 0; index < list.tagCount() && locations.size() < MAX_CHESTS; index++) {
            NBTTagCompound entry = list.getCompoundTagAt(index);
            Location location = new Location(entry.getInteger(KEY_DIMENSION), entry.getLong(KEY_POSITION));
            if (!locations.containsKey(location)) {
                locations.put(location, location);
            }
        }
    }

    /** Stable dimensional identity for one physical single or double-chest half. */
    public static final class Location {
        private static final Comparator<Location> ORDER = new Comparator<Location>() {
            @Override
            public int compare(Location left, Location right) {
                int dimension = Integer.compare(left.dimension, right.dimension);
                return dimension != 0 ? dimension : Long.compare(left.packedPosition, right.packedPosition);
            }
        };

        private final int dimension;
        private final long packedPosition;

        public Location(int dimension, long packedPosition) {
            this.dimension = dimension;
            this.packedPosition = packedPosition;
        }

        public int getDimension() {
            return dimension;
        }

        public BlockPos getPosition() {
            return BlockPos.fromLong(packedPosition);
        }

        public long getPackedPosition() {
            return packedPosition;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Location)) {
                return false;
            }
            Location location = (Location) other;
            return dimension == location.dimension && packedPosition == location.packedPosition;
        }

        @Override
        public int hashCode() {
            int result = dimension;
            result = 31 * result + (int) (packedPosition ^ (packedPosition >>> 32));
            return result;
        }
    }
}
