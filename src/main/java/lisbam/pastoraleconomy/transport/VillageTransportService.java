package lisbam.pastoraleconomy.transport;

import lisbam.pastoraleconomy.data.player.PlayerTransportDataService;
import lisbam.pastoraleconomy.data.player.PlayerTransportNode;
import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import lisbam.pastoraleconomy.merchant.MerchantWorldState;
import lisbam.pastoraleconomy.merchant.StationRecord;
import lisbam.pastoraleconomy.merchant.VillageRecord;
import lisbam.pastoraleconomy.merchant.VillageService;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-side bounded search and activation bridge for legacy 1.12.2 villages. */
public final class VillageTransportService {
    private static final int SEARCH_RADIUS = 8192;
    private static final int SEARCH_STEP = 1024;

    private VillageTransportService() {
    }

    public static VillageTransportCandidate findNearestUnconnected(WorldServer world, EntityPlayerMP player,
                                                                    BlockPos origin) {
        if (world == null || player == null || origin == null || world.provider.getDimension() != 0) {
            return null;
        }
        Map<String, BlockPos> candidates = new HashMap<String, BlockPos>();
        addStructureCandidate(world, origin, candidates);
        // A bounded deterministic probe grid makes the vanilla locator continue beyond
        // the first result without scanning or force-loading the whole world.
        for (int radius = SEARCH_STEP; radius <= SEARCH_RADIUS; radius += SEARCH_STEP) {
            addStructureCandidate(world, origin.add(radius, 0, 0), candidates);
            addStructureCandidate(world, origin.add(-radius, 0, 0), candidates);
            addStructureCandidate(world, origin.add(0, 0, radius), candidates);
            addStructureCandidate(world, origin.add(0, 0, -radius), candidates);
            addStructureCandidate(world, origin.add(radius, 0, radius), candidates);
            addStructureCandidate(world, origin.add(-radius, 0, radius), candidates);
            addStructureCandidate(world, origin.add(radius, 0, -radius), candidates);
            addStructureCandidate(world, origin.add(-radius, 0, -radius), candidates);
        }
        MerchantWorldState state = PastoralWorldData.get(world).getMerchantWorldState();
        for (VillageRecord record : state.getVillages()) {
            candidates.put(key(record.getCenterX(), record.getCenterZ()),
                    new BlockPos(record.getCenterX(), record.getCenterY(), record.getCenterZ()));
        }
        List<BlockPos> ordered = new ArrayList<BlockPos>(candidates.values());
        Collections.sort(ordered, new Comparator<BlockPos>() {
            @Override
            public int compare(BlockPos first, BlockPos second) {
                long a = horizontalSquared(origin, first);
                long b = horizontalSquared(origin, second);
                int byDistance = Long.compare(a, b);
                return byDistance != 0 ? byDistance : key(first.getX(), first.getZ()).compareTo(key(second.getX(), second.getZ()));
            }
        });
        for (BlockPos position : ordered) {
            VillageRecord record = findOrCreatePreviewIdentity(world, position);
            if (record == null) {
                continue;
            }
            StationRecord station = record.getStationId() == null ? null : state.getStation(record.getStationId());
            UUID stationId = station == null ? null : station.getStationId();
            if (stationId != null && isActiveForPlayer(player, stationId)) {
                continue;
            }
            long distance = TransportCost.horizontalDistance(origin, position);
            long fee = TransportCost.connectionFee(distance);
            return new VillageTransportCandidate(record.getVillageId(), stationId, position, distance, fee,
                    "村庄");
        }
        return null;
    }

    public static VillageTransportCandidate findCandidateById(WorldServer world, EntityPlayerMP player,
                                                               BlockPos origin, UUID villageId) {
        if (villageId == null) {
            return null;
        }
        // The preview UUID is only a short-lived hint.  Re-run the bounded,
        // server-side search and accept the request only when this village is
        // still the current nearest unconnected candidate.  Do not fall back
        // to an arbitrary persisted VillageRecord: doing so would allow a
        // stale client snapshot to activate a village that is no longer the
        // advertised target (or one that has since become active).
        VillageTransportCandidate candidate = findNearestUnconnected(world, player, origin);
        return candidate != null && villageId.equals(candidate.getVillageId()) ? candidate : null;
    }

    public static StationRecord activateVillage(WorldServer world, VillageTransportCandidate candidate) {
        if (world == null || candidate == null) {
            return null;
        }
        // Loading only the target structure chunk is bounded and happens after the
        // caller has revalidated the request, never during a GUI preview tick.
        world.getChunkFromBlockCoords(candidate.getPosition());
        VillageRecord record = VillageService.getOrCreateStructureIdentity(world, candidate.getVillageId(),
                candidate.getPosition());
        if (record == null) {
            return null;
        }
        record.updateObservation(record.getCenterX(), record.getCenterY(), record.getCenterZ(),
                Math.max(2, record.getVillagerCount()), true);
        return VillageService.ensureVillageStation(world, record);
    }

    private static VillageRecord findOrCreatePreviewIdentity(WorldServer world, BlockPos position) {
        MerchantWorldState state = PastoralWorldData.get(world).getMerchantWorldState();
        VillageRecord nearest = null;
        long best = Long.MAX_VALUE;
        for (VillageRecord record : state.getVillages()) {
            long distance = horizontalSquared(position,
                    new BlockPos(record.getCenterX(), record.getCenterY(), record.getCenterZ()));
            if (distance <= (long) VillageService.VILLAGE_DEDUP_DISTANCE * VillageService.VILLAGE_DEDUP_DISTANCE
                    && distance < best) {
                best = distance;
                nearest = record;
            }
        }
        if (nearest != null) {
            return nearest;
        }
        UUID identity = UUID.nameUUIDFromBytes((world.getSeed() + ":village:" + position.getX() + ":" + position.getZ())
                .getBytes(StandardCharsets.UTF_8));
        return new VillageRecord(identity, position.getX(), position.getY(), position.getZ());
    }

    private static boolean isActiveForPlayer(EntityPlayerMP player, UUID stationId) {
        PlayerTransportNode node = PlayerTransportDataService.getNode(player, stationId);
        return node != null && node.isActive();
    }

    private static void addStructureCandidate(WorldServer world, BlockPos origin, Map<String, BlockPos> candidates) {
        BlockPos nearest = world.findNearestStructure("Village", origin, false);
        if (nearest != null) {
            candidates.put(key(nearest.getX(), nearest.getZ()), nearest);
        }
    }

    private static long horizontalSquared(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private static String key(int x, int z) {
        return x + ":" + z;
    }
}
