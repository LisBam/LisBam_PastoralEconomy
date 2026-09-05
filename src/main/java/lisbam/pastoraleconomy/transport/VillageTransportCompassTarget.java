package lisbam.pastoraleconomy.transport;

import lisbam.pastoraleconomy.merchant.StationRecord;
import lisbam.pastoraleconomy.merchant.StationRole;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;

/** Pure, deterministic nearest physical-village selection shared by the compass and its regression test. */
public final class VillageTransportCompassTarget {
    private VillageTransportCompassTarget() {
    }

    public static BlockPos findNearest(Collection<StationRecord> villageStationRecords,
                                       Collection<BlockPos> loadedPhysicalStations, int dimension, BlockPos origin) {
        if (origin == null) {
            return null;
        }
        BlockPos nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        if (villageStationRecords != null) {
            for (StationRecord station : villageStationRecords) {
                if (station == null || station.getRole() != StationRole.VILLAGE
                        || station.getDimension() != dimension) {
                    continue;
                }
                BlockPos candidate = station.getPosition();
                long distance = horizontalSquared(origin, candidate);
                if (isCloser(candidate, distance, nearest, nearestDistance)) {
                    nearest = candidate;
                    nearestDistance = distance;
                }
            }
        }
        if (loadedPhysicalStations != null) {
            for (BlockPos candidate : loadedPhysicalStations) {
                if (candidate == null) {
                    continue;
                }
                long distance = horizontalSquared(origin, candidate);
                if (isCloser(candidate, distance, nearest, nearestDistance)) {
                    nearest = candidate.toImmutable();
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    private static boolean isCloser(BlockPos candidate, long candidateDistance, BlockPos current,
                                    long currentDistance) {
        if (current == null || candidateDistance < currentDistance) {
            return true;
        }
        if (candidateDistance != currentDistance) {
            return false;
        }
        if (candidate.getX() != current.getX()) {
            return candidate.getX() < current.getX();
        }
        if (candidate.getZ() != current.getZ()) {
            return candidate.getZ() < current.getZ();
        }
        return candidate.getY() < current.getY();
    }

    private static long horizontalSquared(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }
}
