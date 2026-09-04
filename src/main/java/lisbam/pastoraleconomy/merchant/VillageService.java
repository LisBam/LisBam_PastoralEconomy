package lisbam.pastoraleconomy.merchant;

import lisbam.pastoraleconomy.block.ModBlocks;
import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import lisbam.pastoraleconomy.entity.EntityMerchant;
import lisbam.pastoraleconomy.tile.TileVillageStation;
import lisbam.pastoraleconomy.transport.TransportStationRecord;
import lisbam.pastoraleconomy.transport.TransportStationType;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.Village;
import net.minecraft.world.WorldServer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Bounded Overworld maintenance for legacy 1.12.2 villages. It only observes
 * the vanilla VillageCollection and loaded entities/chunks; it never force-loads land.
 */
public final class VillageService {
    public static final int MINIMUM_VILLAGERS = 2;
    public static final int VILLAGE_REFERENCE_RANGE = 128;
    public static final int VILLAGE_CONNECTION_DISTANCE = 64;
    public static final int VILLAGE_DEDUP_DISTANCE = 160;
    public static final int STATION_SEARCH_RADIUS = 12;
    public static final int MAINTENANCE_INTERVAL_TICKS = 200;
    /** Lets persisted chunk entities join their world before records are allowed to replace them. */
    private static final Map<WorldServer, Long> INITIAL_ENTITY_SETTLE_TICKS = new WeakHashMap<WorldServer, Long>();

    private VillageService() {
    }

    public static void tick(WorldServer overworld) {
        if (overworld == null || overworld.isRemote || overworld.provider.getDimension() != 0) {
            return;
        }
        long worldTime = overworld.getTotalWorldTime();
        if (worldTime < MAINTENANCE_INTERVAL_TICKS || worldTime % MAINTENANCE_INTERVAL_TICKS != 0L) {
            return;
        }
        PastoralWorldData data = PastoralWorldData.get(overworld);
        MerchantWorldState state = data.getMerchantWorldState();
        boolean changed = observeLegacyVillages(overworld, state);
        changed |= ensureStations(overworld, state);
        if (isInitialEntitySettleComplete(overworld, worldTime)) {
            changed |= reconcileMerchants(overworld, state);
        }
        if (changed) {
            data.markDirty();
        }
    }

    /** World load precedes chunk-entity joining; defer its first replacement pass by one maintenance interval. */
    private static boolean isInitialEntitySettleComplete(WorldServer world, long worldTime) {
        Long readyAt = INITIAL_ENTITY_SETTLE_TICKS.get(world);
        if (readyAt == null) {
            INITIAL_ENTITY_SETTLE_TICKS.put(world, worldTime + MAINTENANCE_INTERVAL_TICKS);
            return false;
        }
        return worldTime >= readyAt;
    }

    private static boolean observeLegacyVillages(WorldServer overworld, MerchantWorldState state) {
        boolean changed = false;
        @SuppressWarnings("unchecked")
        List<Village> villages = overworld.getVillageCollection().getVillageList();
        for (Village legacyVillage : villages) {
            int villagers = legacyVillage.getNumVillagers();
            if (villagers < MINIMUM_VILLAGERS) {
                continue;
            }
            BlockPos center = legacyVillage.getCenter();
            if (!overworld.isBlockLoaded(center)) {
                continue;
            }
            VillageRecord record = findMatchingVillage(state.getVillages(), center);
            if (record == null) {
                record = new VillageRecord(UUID.randomUUID(), center.getX(), center.getY(), center.getZ());
                state.putVillage(record);
                changed = true;
            }
            if (!record.isActive() || record.getVillagerCount() != villagers
                    || record.getCenterX() != center.getX() || record.getCenterY() != center.getY()
                    || record.getCenterZ() != center.getZ()) {
                record.updateObservation(center.getX(), center.getY(), center.getZ(), villagers, true);
                changed = true;
            }
        }
        // A persisted village identity remains valid even when its chunk is
        // currently unloaded. Do not retire it merely because VillageCollection
        // only reports loaded/active settlements this tick; this also lets a
        // protected station recover after a temporary chunk unload.
        return changed;
    }

    private static VillageRecord findMatchingVillage(Collection<VillageRecord> records, BlockPos center) {
        VillageRecord connected = null;
        long connectedDistance = Long.MAX_VALUE;
        VillageRecord deduplicated = null;
        long deduplicatedDistance = Long.MAX_VALUE;
        for (VillageRecord record : records) {
            long dx = (long) record.getCenterX() - center.getX();
            long dz = (long) record.getCenterZ() - center.getZ();
            long distance = dx * dx + dz * dz;
            if (distance <= (long) VILLAGE_CONNECTION_DISTANCE * VILLAGE_CONNECTION_DISTANCE
                    && distance < connectedDistance) {
                connected = record;
                connectedDistance = distance;
            }
            if (distance <= (long) VILLAGE_DEDUP_DISTANCE * VILLAGE_DEDUP_DISTANCE
                    && distance < deduplicatedDistance) {
                deduplicated = record;
                deduplicatedDistance = distance;
            }
        }
        // A centre within the 128-block reference range is a normal continuation;
        // the 160-block fallback prevents an old-centre drift from creating a new ID.
        if (connected != null) {
            return connected;
        }
        if (deduplicated != null && deduplicatedDistance <= (long) VILLAGE_REFERENCE_RANGE * VILLAGE_REFERENCE_RANGE) {
            return deduplicated;
        }
        return deduplicated;
    }

    private static boolean ensureStations(WorldServer overworld, MerchantWorldState state) {
        boolean changed = false;
        PastoralWorldData worldData = PastoralWorldData.get(overworld);
        for (VillageRecord village : state.getVillages()) {
            if (!village.isActive()) {
                continue;
            }
            StationRecord station = village.getStationId() == null ? null : state.getStation(village.getStationId());
            if (station == null) {
                station = new StationRecord(UUID.randomUUID(), village.getVillageId(), 0,
                        new BlockPos(village.getCenterX(), village.getCenterY(), village.getCenterZ()), StationRole.VILLAGE);
                state.putStation(station);
                village.setStationId(station.getStationId());
                changed = true;
            }
            if (ensureStationBlock(overworld, village, station)) {
                changed = true;
            }
            TransportStationRecord transportRecord = new TransportStationRecord(station.getStationId(),
                    station.getDimension(), station.getPosition(), TransportStationType.VILLAGE, village.getVillageId());
            if (worldData.getTransportWorldState().putStation(transportRecord)) {
                changed = true;
            }
        }
        return changed;
    }

    private static boolean ensureStationBlock(WorldServer world, VillageRecord village, StationRecord station) {
        if (station.getDimension() != 0) {
            return false;
        }
        BlockPos recordedPosition = station.getPosition();
        if (!world.isBlockLoaded(recordedPosition)) {
            return false;
        }
        if (isCorrectStation(world, recordedPosition, village, station)) {
            return false;
        }
        BlockPos placement = findSafeStationPosition(world, village, recordedPosition);
        if (placement == null) {
            return false;
        }
        if (!world.setBlockState(placement, ModBlocks.VILLAGE_STATION.getDefaultState(), 3)) {
            return false;
        }
        TileEntity tile = world.getTileEntity(placement);
        if (!(tile instanceof TileVillageStation)) {
            world.setBlockToAir(placement);
            return false;
        }
        ((TileVillageStation) tile).bind(station.getStationId(), village.getVillageId(), StationRole.VILLAGE);
        station.moveTo(0, placement);
        return true;
    }

    private static boolean isCorrectStation(WorldServer world, BlockPos position, VillageRecord village,
                                            StationRecord station) {
        if (world.getBlockState(position).getBlock() != ModBlocks.VILLAGE_STATION) {
            return false;
        }
        TileEntity tile = world.getTileEntity(position);
        if (!(tile instanceof TileVillageStation)) {
            return false;
        }
        TileVillageStation stationTile = (TileVillageStation) tile;
        if (station.getStationId().equals(stationTile.getStationId()) && village.getVillageId().equals(stationTile.getVillageId())) {
            return true;
        }
        if (!stationTile.isVillageStation()) {
            stationTile.bind(station.getStationId(), village.getVillageId(), StationRole.VILLAGE);
            return true;
        }
        return false;
    }

    private static BlockPos findSafeStationPosition(WorldServer world, VillageRecord village, BlockPos recordedPosition) {
        if (isSafeStationPosition(world, recordedPosition)) {
            return recordedPosition;
        }
        BlockPos center = new BlockPos(village.getCenterX(), village.getCenterY(), village.getCenterZ());
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;
        for (int offsetX = -STATION_SEARCH_RADIUS; offsetX <= STATION_SEARCH_RADIUS; offsetX++) {
            for (int offsetZ = -STATION_SEARCH_RADIUS; offsetZ <= STATION_SEARCH_RADIUS; offsetZ++) {
                long distance = (long) offsetX * offsetX + (long) offsetZ * offsetZ;
                if (distance > (long) STATION_SEARCH_RADIUS * STATION_SEARCH_RADIUS || distance >= bestDistance) {
                    continue;
                }
                BlockPos column = new BlockPos(center.getX() + offsetX, 0, center.getZ() + offsetZ);
                if (!world.isBlockLoaded(column)) {
                    continue;
                }
                BlockPos candidate = world.getHeight(column);
                if (isSafeStationPosition(world, candidate)) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private static boolean isSafeStationPosition(WorldServer world, BlockPos position) {
        if (position.getY() <= 0 || position.getY() >= world.getHeight() - 2 || !world.isBlockLoaded(position)
                || !world.isAirBlock(position) || !world.isAirBlock(position.up())) {
            return false;
        }
        BlockPos below = position.down();
        IBlockState floor = world.getBlockState(below);
        if (!floor.getMaterial().isSolid() || floor.getBlock() == Blocks.FARMLAND
                || floor.getBlock() instanceof BlockDoor || world.getTileEntity(position) != null
                || world.getTileEntity(below) != null) {
            return false;
        }
        return floor.isSideSolid(world, below, EnumFacing.UP);
    }

    private static boolean reconcileMerchants(WorldServer world, MerchantWorldState state) {
        MerchantEntityIndex entityIndex = indexAndRemoveInvalidMerchants(world, state);
        boolean changed = entityIndex.changed;
        Map<UUID, EntityMerchant> entities = entityIndex.entities;
        for (VillageRecord village : state.getVillages()) {
            if (!village.isActive()) {
                continue;
            }
            StationRecord station = village.getStationId() == null ? null : state.getStation(village.getStationId());
            if (station == null || station.getDimension() != 0 || !world.isBlockLoaded(station.getPosition())
                    || !isCorrectStation(world, station.getPosition(), village, station)) {
                continue;
            }
            int target = getTargetMerchantCount(village.getVillagerCount());
            int active = 0;
            for (UUID merchantId : village.getMerchantRoster()) {
                MerchantRecord merchant = state.getMerchant(merchantId);
                if (merchant == null) {
                    continue;
                }
                boolean shouldBeActive = active < target;
                if (merchant.isActive() != shouldBeActive) {
                    merchant.setActive(shouldBeActive);
                    changed = true;
                }
                if (shouldBeActive) {
                    active++;
                } else {
                    EntityMerchant excess = entities.get(merchantId);
                    if (excess != null) {
                        excess.setDead();
                        entities.remove(merchantId);
                    }
                }
            }
            while (active < target) {
                MerchantRecord merchant = new MerchantRecord(UUID.randomUUID(), village.getVillageId(), station.getStationId());
                state.putMerchant(merchant);
                village.addMerchant(merchant.getMerchantId());
                active++;
                changed = true;
            }
            for (UUID merchantId : village.getMerchantRoster()) {
                MerchantRecord merchant = state.getMerchant(merchantId);
                if (merchant == null || !merchant.isActive() || entities.containsKey(merchantId)) {
                    continue;
                }
                if (merchant.hasKnownEntityChunk() && !isKnownEntityChunkLoaded(world, merchant)) {
                    continue;
                }
                EntityMerchant spawned = spawnMerchant(world, merchant, station);
                if (spawned != null) {
                    entities.put(merchantId, spawned);
                    changed |= merchant.updateKnownEntityChunk(spawned.getPosition().getX() >> 4,
                            spawned.getPosition().getZ() >> 4);
                }
            }
        }
        return changed;
    }

    /** One maintenance pass replaces the previous copy-plus-second full entity scan. */
    private static MerchantEntityIndex indexAndRemoveInvalidMerchants(WorldServer world, MerchantWorldState state) {
        boolean changed = false;
        Set<UUID> seen = new HashSet<UUID>();
        Map<UUID, EntityMerchant> result = new HashMap<UUID, EntityMerchant>();
        for (Object value : world.loadedEntityList) {
            if (!(value instanceof EntityMerchant)) {
                continue;
            }
            EntityMerchant merchant = (EntityMerchant) value;
            UUID merchantId = merchant.getMerchantId();
            MerchantRecord record = merchantId == null ? null : state.getMerchant(merchantId);
            if (!isCurrentMerchantEntity(state, merchant, record) || !seen.add(merchantId)) {
                merchant.setDead();
                changed = true;
            } else if (!merchant.isDead) {
                result.put(merchantId, merchant);
                changed |= record.updateKnownEntityChunk(merchant.getPosition().getX() >> 4,
                        merchant.getPosition().getZ() >> 4);
            }
        }
        return new MerchantEntityIndex(result, changed);
    }

    /** Reject stale, inactive, or cross-village entity NBT before it can count toward a roster. */
    private static boolean isCurrentMerchantEntity(MerchantWorldState state, EntityMerchant merchant,
                                                   MerchantRecord record) {
        if (state == null || merchant == null || record == null || !record.isActive() || !merchant.hasValidBinding()
                || !record.getMerchantId().equals(merchant.getMerchantId())
                || !record.getVillageId().equals(merchant.getVillageId())
                || !record.getStationId().equals(merchant.getStationId())) {
            return false;
        }
        VillageRecord village = state.getVillage(record.getVillageId());
        StationRecord station = state.getStation(record.getStationId());
        return village != null && village.getMerchantRoster().contains(record.getMerchantId())
                && station != null && station.getVillageId().equals(record.getVillageId());
    }

    /** An unloaded last-known entity chunk may still contain the persistent entity in its chunk NBT. */
    private static boolean isKnownEntityChunkLoaded(WorldServer world, MerchantRecord merchant) {
        return world.isBlockLoaded(new BlockPos(merchant.getKnownEntityChunkX() << 4, 0,
                merchant.getKnownEntityChunkZ() << 4));
    }

    private static EntityMerchant spawnMerchant(WorldServer world, MerchantRecord merchant, StationRecord station) {
        if (!world.isBlockLoaded(station.getPosition())) {
            return null;
        }
        BlockPos spawn = findMerchantSpawnPosition(world, station.getPosition());
        if (spawn == null) {
            return null;
        }
        EntityMerchant entity = new EntityMerchant(world);
        entity.bind(merchant.getMerchantId(), merchant.getVillageId(), merchant.getStationId(), station.getPosition());
        entity.setPosition(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        return world.spawnEntity(entity) ? entity : null;
    }

    private static BlockPos findMerchantSpawnPosition(WorldServer world, BlockPos station) {
        for (int radius = 0; radius <= STATION_SEARCH_RADIUS; radius++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                    if (Math.abs(offsetX) != radius && Math.abs(offsetZ) != radius) {
                        continue;
                    }
                    BlockPos candidate = station.add(offsetX, 1, offsetZ);
                    if (!world.isBlockLoaded(candidate) || !world.isAirBlock(candidate) || !world.isAirBlock(candidate.up())) {
                        continue;
                    }
                    IBlockState floor = world.getBlockState(candidate.down());
                    if (floor.getMaterial().isSolid() && floor.isSideSolid(world, candidate.down(), EnumFacing.UP)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    public static int getTargetMerchantCount(int villagerCount) {
        long villagers = Math.max(0L, (long) villagerCount);
        long target = (villagers + 4L) / 5L;
        return (int) Math.min((long) Integer.MAX_VALUE, Math.max(3L, target));
    }

    private static final class MerchantEntityIndex {
        private final Map<UUID, EntityMerchant> entities;
        private final boolean changed;

        private MerchantEntityIndex(Map<UUID, EntityMerchant> entities, boolean changed) {
            this.entities = entities;
            this.changed = changed;
        }
    }

    /** Returns an existing persisted identity near a structure result, or creates one without a station block. */
    public static VillageRecord getOrCreateStructureIdentity(WorldServer world, UUID preferredId, BlockPos center) {
        if (world == null || center == null) {
            return null;
        }
        PastoralWorldData data = PastoralWorldData.get(world);
        MerchantWorldState state = data.getMerchantWorldState();
        if (preferredId != null) {
            VillageRecord byId = state.getVillage(preferredId);
            if (byId != null) {
                return byId;
            }
        }
        VillageRecord nearest = null;
        long best = Long.MAX_VALUE;
        for (VillageRecord record : state.getVillages()) {
            long dx = (long) record.getCenterX() - center.getX();
            long dz = (long) record.getCenterZ() - center.getZ();
            long distance = dx * dx + dz * dz;
            if (distance <= (long) VILLAGE_DEDUP_DISTANCE * VILLAGE_DEDUP_DISTANCE && distance < best) {
                best = distance;
                nearest = record;
            }
        }
        if (nearest != null) {
            return nearest;
        }
        UUID id = preferredId == null ? UUID.randomUUID() : preferredId;
        VillageRecord created = new VillageRecord(id, center.getX(), center.getY(), center.getZ());
        state.putVillage(created);
        data.markDirty();
        return created;
    }

    /** Idempotently creates the one world station for a village, then mirrors it into transport registry. */
    public static StationRecord ensureVillageStation(WorldServer world, VillageRecord village) {
        if (world == null || village == null || world.provider.getDimension() != 0) {
            return null;
        }
        PastoralWorldData data = PastoralWorldData.get(world);
        MerchantWorldState state = data.getMerchantWorldState();
        StationRecord station = village.getStationId() == null ? null : state.getStation(village.getStationId());
        if (station == null) {
            station = state.getStationByVillage(village.getVillageId());
            if (station != null) {
                village.setStationId(station.getStationId());
            }
        }
        if (station == null) {
            station = new StationRecord(UUID.randomUUID(), village.getVillageId(), 0,
                    new BlockPos(village.getCenterX(), village.getCenterY(), village.getCenterZ()), StationRole.VILLAGE);
            state.putStation(station);
            village.setStationId(station.getStationId());
            data.markDirty();
        }
        if (!ensureStationBlock(world, village, station)) {
            // It may already be correct; do not treat a missing/unloaded block as a new identity.
            if (!isCorrectStation(world, station.getPosition(), village, station)) {
                return null;
            }
        }
        TransportStationRecord transport = new TransportStationRecord(station.getStationId(), 0,
                station.getPosition(), TransportStationType.VILLAGE, village.getVillageId());
        if (data.getTransportWorldState().putStation(transport)) {
            data.markDirty();
        }
        return station;
    }
}
