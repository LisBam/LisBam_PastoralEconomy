package lisbam.pastoraleconomy.transport;

import net.minecraft.util.math.BlockPos;

import java.util.Collection;

/** Pure, deterministic nearest-node selection shared by the compass and its regression test. */
public final class VillageTransportCompassTarget {
    private VillageTransportCompassTarget() {
    }

    public static TransportStationRecord findNearest(Collection<TransportStationRecord> stations, int dimension,
                                                      BlockPos origin) {
        if (stations == null || origin == null) {
            return null;
        }
        TransportStationRecord nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        for (TransportStationRecord station : stations) {
            if (station == null || station.getType() != TransportStationType.VILLAGE
                    || station.getDimension() != dimension) {
                continue;
            }
            long distance = horizontalSquared(origin, station.getPosition());
            if (nearest == null || distance < nearestDistance
                    || (distance == nearestDistance && station.getStationId().toString()
                    .compareTo(nearest.getStationId().toString()) < 0)) {
                nearest = station;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static long horizontalSquared(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }
}
