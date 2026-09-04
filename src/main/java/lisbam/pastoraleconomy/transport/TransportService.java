package lisbam.pastoraleconomy.transport;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.block.ModBlocks;
import lisbam.pastoraleconomy.data.player.CoinService;
import lisbam.pastoraleconomy.data.player.PlayerTransportDataService;
import lisbam.pastoraleconomy.data.player.PlayerTransportNode;
import lisbam.pastoraleconomy.data.world.PastoralWorldData;
import lisbam.pastoraleconomy.gui.ContainerTransportStation;
import lisbam.pastoraleconomy.gui.GuiIds;
import lisbam.pastoraleconomy.network.ModNetwork;
import lisbam.pastoraleconomy.network.message.SyncTransportStateMessage;
import lisbam.pastoraleconomy.tile.TileTransportStation;
import lisbam.pastoraleconomy.tile.TileVillageStation;
import lisbam.pastoraleconomy.merchant.StationRecord;
import lisbam.pastoraleconomy.merchant.MerchantWorldState;
import lisbam.pastoraleconomy.merchant.VillageService;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import java.util.Comparator;

/** Logical-server authority for transport node placement, activation, aliases, and GUI snapshots. */
public final class TransportService {
    public static final int OVERWORLD_DIMENSION = 0;

    private TransportService() {
    }

    public static void grantStarterTransportIfNeeded(EntityPlayerMP player) {
        if (PlayerTransportDataService.hasStarterTransportBeenGranted(player)) {
            return;
        }
        ItemStack stack = StarterTransportItemData.createStarterStack(player.getUniqueID());
        boolean inserted = player.inventory.addItemStackToInventory(stack);
        if (!inserted) {
            EntityItem dropped = player.entityDropItem(stack, 0.0F);
            if (dropped == null) {
                return;
            }
            dropped.setDefaultPickupDelay();
        }
        PlayerTransportDataService.markStarterTransportGranted(player);
    }

    /** Called after normal placement, never trusting any copied TileEntity station UUID. */
    public static void onStationPlaced(World world, BlockPos pos, TileTransportStation tile, ItemStack sourceStack) {
        if (world.isRemote) {
            return;
        }
        tile.setStarterOwner(StarterTransportItemData.getStarterOwner(sourceStack));
        onStationTileLoaded(world, pos, tile);
    }

    /** Assigns a station identity or resolves copied-NBT UUID duplication without force-loading chunks. */
    public static void onStationTileLoaded(World world, BlockPos pos, TileTransportStation tile) {
        if (world == null || world.isRemote || pos == null || tile == null
                || world.getBlockState(pos).getBlock() != ModBlocks.TRANSPORT_STATION) {
            return;
        }
        PastoralWorldData data = PastoralWorldData.get(world);
        TransportWorldState state = data.getTransportWorldState();
        int dimension = world.provider.getDimension();
        TransportStationRecord record = state.getStation(tile.getStationId());
        if (record != null && record.isAt(dimension, pos)) {
            return;
        }

        if (record != null) {
            // A copied TileEntity carried an ID already registered elsewhere.
            tile.assignStationId(createUniqueStationId(state));
        } else {
            TransportStationRecord atPosition = state.getStationAt(dimension, pos);
            if (atPosition != null) {
                tile.assignStationId(atPosition.getStationId());
            } else if (tile.getStationId() == null) {
                tile.assignStationId(createUniqueStationId(state));
            }
        }

        TransportStationRecord replacement = new TransportStationRecord(tile.getStationId(), dimension, pos,
                TransportStationType.SELF_BUILT);
        if (state.putStation(replacement)) {
            data.markDirty();
        }
    }

    public static void unregisterBrokenStation(World world, BlockPos pos, TileTransportStation tile) {
        if (world == null || world.isRemote || tile == null || tile.getStationId() == null) {
            return;
        }
        PastoralWorldData data = PastoralWorldData.get(world);
        if (data.getTransportWorldState().removeStation(tile.getStationId(), world.provider.getDimension(), pos)) {
            data.markDirty();
        }
    }

    public static void openStationGui(EntityPlayerMP player, World world, BlockPos pos, TileTransportStation tile) {
        openStationGui(player, world, pos, (TileEntity) tile);
    }

    public static void openStationGui(EntityPlayerMP player, World world, BlockPos pos, TileEntity tile) {
        if (player == null || world == null || world.isRemote || tile == null) {
            return;
        }
        UUID stationId = tile instanceof TileTransportStation ? ((TileTransportStation) tile).getStationId()
                : tile instanceof TileVillageStation && ((TileVillageStation) tile).isVillageStation()
                ? ((TileVillageStation) tile).getStationId() : null;
        if (stationId == null) {
            return;
        }
        if (tile instanceof TileVillageStation) {
            TileVillageStation villageTile = (TileVillageStation) tile;
            MerchantWorldState merchantState = PastoralWorldData.get(world).getMerchantWorldState();
            StationRecord merchantStation = merchantState.getStation(stationId);
            if (merchantStation != null) {
                PastoralWorldData.get(world).getTransportWorldState().putStation(new TransportStationRecord(
                        stationId, 0, pos, TransportStationType.VILLAGE, villageTile.getVillageId()));
                PastoralWorldData.get(world).markDirty();
            }
        }
        player.openGui(LisBamPastoralEconomy.INSTANCE, GuiIds.TRANSPORT_STATION, world,
                pos.getX(), pos.getY(), pos.getZ());
        syncOpenStation(player);
    }

    public static void handleRefresh(EntityPlayerMP player, UUID stationId) {
        if (isOpenStation(player, stationId) != null) {
            syncOpenStation(player);
        }
    }

    public static void handleConnect(EntityPlayerMP player, UUID stationId) {
        TileEntity tile = isOpenStation(player, stationId);
        if (tile == null || !isLiveStation(player.world, stationId, tile)) {
            return;
        }
        if (player.world.provider.getDimension() != OVERWORLD_DIMENSION) {
            syncOpenStation(player);
            return;
        }
        PlayerTransportNode existing = PlayerTransportDataService.getNode(player, stationId);
        if (existing != null && existing.isActive() || !PlayerTransportDataService.canActivateNode(player, stationId)) {
            syncOpenStation(player);
            return;
        }

        boolean firstSelfBuilt = !PlayerTransportDataService.hasEstablishedFirstSelfBuiltStation(player);
        boolean free = firstSelfBuilt && tile instanceof TileTransportStation
                && !PlayerTransportDataService.hasUsedFirstSelfBuiltStationFree(player)
                && ((TileTransportStation) tile).isStarterFor(player.getUniqueID());
        long fee = free ? 0L : findConnectionFee(player, stationId);
        if (!free && !CoinService.trySpend(player, fee)) {
            syncOpenStation(player);
            return;
        }

        String alias = existing == null
                ? (tile instanceof TileVillageStation ? createVillageAlias(player) : createDefaultAlias(player, firstSelfBuilt))
                : existing.getAlias();
        if (!PlayerTransportDataService.activateNode(player, stationId, alias)) {
            if (!free) {
                CoinService.addCoins(player, fee);
            }
            syncOpenStation(player);
            return;
        }
        if (existing == null && tile instanceof TileTransportStation && !firstSelfBuilt) {
            PlayerTransportDataService.takeNextSelfBuiltStationSequence(player);
        }
        if (tile instanceof TileTransportStation && firstSelfBuilt) {
            PlayerTransportDataService.markFirstSelfBuiltStationEstablished(player, free);
        }
        syncOpenStation(player);
    }

    public static void handleRemove(EntityPlayerMP player, UUID stationId) {
        if (hasOpenTransportStation(player) && stationId != null) {
            PlayerTransportDataService.deactivateNode(player, stationId);
        }
        syncOpenStation(player);
    }

    public static void handleRename(EntityPlayerMP player, UUID stationId, String alias) {
        PlayerTransportNode node = PlayerTransportDataService.getNode(player, stationId);
        if (hasOpenTransportStation(player) && node != null && TransportNameRules.isValidAlias(alias)
                && TransportNameRules.isUniqueAlias(alias,
                PlayerTransportDataService.getAliasesExcept(player, stationId), null)) {
            PlayerTransportDataService.renameNode(player, stationId, alias);
        }
        syncOpenStation(player);
    }

    public static void handleFindVillage(EntityPlayerMP player, UUID currentStationId) {
        if (isOpenStation(player, currentStationId) == null || !isPlayerAtActiveOverworldStation(player, currentStationId)) {
            syncOpenStation(player);
            return;
        }
        syncOpenStation(player);
    }

    public static void handleConnectVillage(EntityPlayerMP player, UUID villageId) {
        TileEntity sourceTile = isOpenStation(player, currentOpenStation(player));
        if (sourceTile == null || !isPlayerAtActiveOverworldStation(player, currentOpenStation(player))) {
            syncOpenStation(player);
            return;
        }
        if (!(player.world instanceof WorldServer) || player.world.provider.getDimension() != OVERWORLD_DIMENSION) {
            syncOpenStation(player);
            return;
        }
        ContainerTransportStation container = (ContainerTransportStation) player.openContainer;
        VillageTransportCandidate candidate = VillageTransportService.findCandidateById((WorldServer) player.world,
                player, container.getPosition(), villageId);
        if (candidate == null) {
            syncOpenStation(player);
            return;
        }
        TransportStationRecord source = PastoralWorldData.get(player.world).getTransportWorldState()
                .getStation(container.getStationId());
        if (source == null) {
            syncOpenStation(player);
            return;
        }
        BlockPos targetPosition = candidate.getPosition();
        if (candidate.getStationId() != null) {
            TransportStationRecord targetRecord = PastoralWorldData.get(player.world).getTransportWorldState()
                    .getStation(candidate.getStationId());
            if (targetRecord != null) {
                targetPosition = targetRecord.getPosition();
            }
        }
        long fee = TransportCost.connectionFee(TransportCost.horizontalDistance(source.getPosition(), targetPosition));
        if (!PlayerTransportDataService.canActivateNode(player, candidate.getStationId() == null ? villageId : candidate.getStationId())) {
            syncOpenStation(player);
            return;
        }
        if (!CoinService.canAfford(player, fee)) {
            syncOpenStation(player);
            return;
        }
        StationRecord villageStation = VillageTransportService.activateVillage((WorldServer) player.world, candidate);
        if (villageStation == null) {
            syncOpenStation(player);
            return;
        }
        UUID stationId = villageStation.getStationId();
        PlayerTransportNode existing = PlayerTransportDataService.getNode(player, stationId);
        if (existing != null && existing.isActive()) {
            syncOpenStation(player);
            return;
        }
        fee = TransportCost.connectionFee(TransportCost.horizontalDistance(source.getPosition(), villageStation.getPosition()));
        if (!CoinService.trySpend(player, fee)) {
            syncOpenStation(player);
            return;
        }
        String alias = existing == null ? createVillageAlias(player) : existing.getAlias();
        if (!PlayerTransportDataService.activateNode(player, stationId, alias)) {
            CoinService.addCoins(player, fee);
        }
        syncOpenStation(player);
    }

    public static void handleTravel(EntityPlayerMP player, UUID destinationId) {
        UUID sourceId = currentOpenStation(player);
        TileEntity sourceTile = isOpenStation(player, sourceId);
        if (sourceTile == null || destinationId == null || !isPlayerAtActiveOverworldStation(player, sourceId)) {
            syncOpenStation(player);
            return;
        }
        if (!(player.world instanceof WorldServer) || player.world.provider.getDimension() != OVERWORLD_DIMENSION) {
            syncOpenStation(player);
            return;
        }
        TransportWorldState state = PastoralWorldData.get(player.world).getTransportWorldState();
        TransportStationRecord source = state.getStation(sourceId);
        TransportStationRecord destination = state.getStation(destinationId);
        PlayerTransportNode destinationNode = PlayerTransportDataService.getNode(player, destinationId);
        if (source == null || destination == null || sourceId.equals(destinationId) || destination.getDimension() != OVERWORLD_DIMENSION
                || destinationNode == null || !destinationNode.isActive()
                || !isRecordUsableForNetwork(player.world, destination)) {
            syncOpenStation(player);
            return;
        }
        WorldServer overworld = (WorldServer) player.world;
        overworld.getChunkFromBlockCoords(destination.getPosition());
        BlockPos landing = findSafeLanding(overworld, destination.getPosition(), player);
        if (landing == null) {
            syncOpenStation(player);
            return;
        }
        long fee = TransportCost.travelFee(TransportCost.horizontalDistance(source.getPosition(), destination.getPosition()));
        if (!CoinService.trySpend(player, fee)) {
            syncOpenStation(player);
            return;
        }
        try {
            player.setPositionAndUpdate(landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D);
            if (player.getDistanceSq(landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D) > 4.0D) {
                CoinService.addCoins(player, fee);
            }
        } catch (RuntimeException failure) {
            CoinService.addCoins(player, fee);
        }
        syncOpenStation(player);
    }

    public static long findConnectionFee(EntityPlayerMP player, UUID currentStationId) {
        if (player == null || currentStationId == null) {
            return TransportCost.connectionFee(0L);
        }
        TransportStationRecord current = PastoralWorldData.get(player.world).getTransportWorldState()
                .getStation(currentStationId);
        if (current == null || current.getDimension() != OVERWORLD_DIMENSION) {
            return TransportCost.connectionFee(0L);
        }
        TransportStationRecord nearest = findNearestActiveStation(player, current);
        return nearest == null ? TransportCost.connectionFee(0L)
                : TransportCost.connectionFee(TransportCost.horizontalDistance(current.getPosition(), nearest.getPosition()));
    }

    public static void syncOpenStation(EntityPlayerMP player) {
        if (player == null || player.world == null || player.world.isRemote
                || !(player.openContainer instanceof ContainerTransportStation)) {
            return;
        }
        ContainerTransportStation container = (ContainerTransportStation) player.openContainer;
        TransportStateSnapshot snapshot = createSnapshot(player, container.getStationId());
        ModNetwork.CHANNEL.sendTo(new SyncTransportStateMessage(snapshot), player);
    }

    private static TransportStateSnapshot createSnapshot(EntityPlayerMP player, UUID currentStationId) {
        PastoralWorldData data = PastoralWorldData.get(player.world);
        TransportWorldState state = data.getTransportWorldState();
        TransportStationRecord current = state.getStation(currentStationId);
        PlayerTransportNode currentNode = PlayerTransportDataService.getNode(player, currentStationId);
        boolean currentExists = current != null && isLiveStation(player.world, currentStationId, null);
        boolean supported = currentExists && current.getDimension() == OVERWORLD_DIMENSION;
        boolean active = currentNode != null && currentNode.isActive() && currentExists;
        TileEntity liveTile = current == null ? null : player.world.getTileEntity(current.getPosition());
        boolean starterFree = supported && !active && liveTile instanceof TileTransportStation
                && !PlayerTransportDataService.hasEstablishedFirstSelfBuiltStation(player)
                && !PlayerTransportDataService.hasUsedFirstSelfBuiltStationFree(player)
                && ((TileTransportStation) liveTile).isStarterFor(player.getUniqueID());
        long fee = supported && !active ? (starterFree ? 0L : findConnectionFee(player, currentStationId)) : 0L;
        int dimension = current == null ? player.world.provider.getDimension() : current.getDimension();
        BlockPos position = current == null ? player.getPosition() : current.getPosition();
        List<TransportNodeView> nodes = new ArrayList<TransportNodeView>();
        for (PlayerTransportNode node : PlayerTransportDataService.getNodes(player)) {
            TransportStationRecord record = state.getStation(node.getStationId());
            if (record == null) {
                nodes.add(new TransportNodeView(node.getStationId(), node.isActive(), node.getAlias(), false,
                        0, 0, 0, 0, TransportStationType.SELF_BUILT, -1L));
            } else {
                BlockPos nodePosition = record.getPosition();
                boolean isDestination = supported && active && node.isActive()
                        && !node.getStationId().equals(currentStationId)
                        && record.getDimension() == OVERWORLD_DIMENSION
                        && isRecordUsableForNetwork(player.world, record);
                long travelFee = isDestination
                        ? TransportCost.travelFee(TransportCost.horizontalDistance(position, nodePosition)) : -1L;
                nodes.add(new TransportNodeView(node.getStationId(), node.isActive(), node.getAlias(), true,
                        record.getDimension(), nodePosition.getX(), nodePosition.getY(), nodePosition.getZ(),
                        record.getType(), travelFee));
            }
        }
        VillageTransportCandidate nearestVillage = supported && active && player.world instanceof WorldServer
                ? VillageTransportService.findNearestUnconnected((WorldServer) player.world, player, position) : null;
        return new TransportStateSnapshot(currentStationId, currentNode == null ? "" : currentNode.getAlias(),
                currentExists, supported, active, starterFree, dimension, position.getX(), position.getY(),
                position.getZ(), CoinService.getBalance(player), fee, nodes, nearestVillage);
    }

    private static TileEntity isOpenStation(EntityPlayerMP player, UUID stationId) {
        if (player == null || stationId == null || !(player.openContainer instanceof ContainerTransportStation)) {
            return null;
        }
        ContainerTransportStation container = (ContainerTransportStation) player.openContainer;
        if (!stationId.equals(container.getStationId()) || !container.canInteractWith(player)) {
            return null;
        }
        TileEntity tile = player.world.getTileEntity(container.getPosition());
        return tile instanceof TileTransportStation || tile instanceof TileVillageStation ? tile : null;
    }

    private static boolean isLiveStation(World world, UUID stationId, TileEntity expectedTile) {
        if (world == null || world.isRemote || stationId == null) {
            return false;
        }
        TransportStationRecord record = PastoralWorldData.get(world).getTransportWorldState().getStation(stationId);
        if (record == null || record.getDimension() != world.provider.getDimension()) {
            return false;
        }
        TileEntity tile = world.getTileEntity(record.getPosition());
        if (tile == null || tile != (expectedTile == null ? tile : expectedTile)) {
            return false;
        }
        if (record.getType() == TransportStationType.VILLAGE) {
            return world.getBlockState(record.getPosition()).getBlock() == ModBlocks.VILLAGE_STATION
                    && tile instanceof TileVillageStation && ((TileVillageStation) tile).isVillageStation()
                    && stationId.equals(((TileVillageStation) tile).getStationId());
        }
        return world.getBlockState(record.getPosition()).getBlock() == ModBlocks.TRANSPORT_STATION
                && tile instanceof TileTransportStation && stationId.equals(((TileTransportStation) tile).getStationId());
    }

    private static TransportStationRecord findNearestActiveStation(EntityPlayerMP player,
                                                                    TransportStationRecord current) {
        TransportWorldState state = PastoralWorldData.get(player.world).getTransportWorldState();
        TransportStationRecord nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        for (PlayerTransportNode node : PlayerTransportDataService.getNodes(player)) {
            if (!node.isActive()) {
                continue;
            }
            TransportStationRecord candidate = state.getStation(node.getStationId());
            if (candidate == null || candidate.getDimension() != OVERWORLD_DIMENSION
                    || !isRecordUsableForNetwork(player.world, candidate)) {
                continue;
            }
            long distance = TransportCost.horizontalDistance(current.getPosition(), candidate.getPosition());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static String createDefaultAlias(EntityPlayerMP player, boolean firstStation) {
        Collection<String> aliases = PlayerTransportDataService.getAliasesExcept(player, null);
        return firstStation ? TransportNameRules.nextHomeAlias(aliases)
                : TransportNameRules.selfBuiltAlias(PlayerTransportDataService.peekNextSelfBuiltStationSequence(player), aliases);
    }

    /** A loaded missing/mismatched TileEntity is invalid; an unloaded chunk remains a valid registry node. */
    private static boolean isRecordUsableForNetwork(World world, TransportStationRecord record) {
        if (world == null || world.isRemote || record == null || record.getDimension() != world.provider.getDimension()) {
            return false;
        }
        BlockPos position = record.getPosition();
        if (!world.isBlockLoaded(position)) {
            return true;
        }
        TileEntity tile = world.getTileEntity(position);
        if (record.getType() == TransportStationType.VILLAGE) {
            return world.getBlockState(position).getBlock() == ModBlocks.VILLAGE_STATION
                    && tile instanceof TileVillageStation && ((TileVillageStation) tile).isVillageStation()
                    && record.getStationId().equals(((TileVillageStation) tile).getStationId());
        }
        return world.getBlockState(position).getBlock() == ModBlocks.TRANSPORT_STATION
                && tile instanceof TileTransportStation && record.getStationId().equals(((TileTransportStation) tile).getStationId());
    }

    private static boolean hasOpenTransportStation(EntityPlayerMP player) {
        return player != null && player.openContainer instanceof ContainerTransportStation
                && ((ContainerTransportStation) player.openContainer).canInteractWith(player);
    }

    private static UUID currentOpenStation(EntityPlayerMP player) {
        return player != null && player.openContainer instanceof ContainerTransportStation
                ? ((ContainerTransportStation) player.openContainer).getStationId() : null;
    }

    private static boolean isPlayerAtActiveOverworldStation(EntityPlayerMP player, UUID stationId) {
        if (player == null || stationId == null || player.world.provider.getDimension() != OVERWORLD_DIMENSION) {
            return false;
        }
        TileEntity tile = isOpenStation(player, stationId);
        if (tile == null || !isLiveStation(player.world, stationId, tile)) {
            return false;
        }
        PlayerTransportNode node = PlayerTransportDataService.getNode(player, stationId);
        return node != null && node.isActive();
    }

    private static String createVillageAlias(EntityPlayerMP player) {
        int sequence = 1;
        Collection<String> aliases = PlayerTransportDataService.getAliasesExcept(player, null);
        while (sequence < Integer.MAX_VALUE) {
            String candidate = "村庄 #" + String.format(java.util.Locale.ROOT, "%03d", sequence);
            if (TransportNameRules.isUniqueAlias(candidate, aliases, null)) {
                return candidate;
            }
            sequence++;
        }
        throw new IllegalStateException("No unique village alias remains.");
    }

    /** Deterministic bounded safe-landing search around a station. */
    public static BlockPos findSafeLanding(WorldServer world, BlockPos station, EntityPlayerMP player) {
        if (world == null || station == null || player == null) {
            return null;
        }
        List<BlockPos> candidates = new ArrayList<BlockPos>();
        for (int dy = -2; dy <= 4; dy++) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    if ((long) dx * dx + (long) dz * dz <= 25L) {
                        candidates.add(station.add(dx, dy, dz));
                    }
                }
            }
        }
        Collections.sort(candidates, new Comparator<BlockPos>() {
            @Override
            public int compare(BlockPos first, BlockPos second) {
                long a = horizontalSquared(station, first);
                long b = horizontalSquared(station, second);
                int byHorizontal = Long.compare(a, b);
                return byHorizontal != 0 ? byHorizontal : Integer.compare(first.getY(), second.getY());
            }
        });
        for (BlockPos candidate : candidates) {
            if (isSafeLanding(world, candidate, player)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isSafeLanding(WorldServer world, BlockPos feet, EntityPlayerMP player) {
        if (feet.getY() <= 0 || feet.getY() + 1 >= world.getHeight()) {
            return false;
        }
        BlockPos floorPos = feet.down();
        net.minecraft.block.state.IBlockState floor = world.getBlockState(floorPos);
        Block floorBlock = floor.getBlock();
        if (isHazard(floorBlock, floor.getMaterial())
                || !floor.getMaterial().isSolid() || !floor.isSideSolid(world, floorPos, net.minecraft.util.EnumFacing.UP)) {
            return false;
        }
        if (isHazard(world.getBlockState(feet).getBlock(), world.getBlockState(feet).getMaterial())
                || isHazard(world.getBlockState(feet.up()).getBlock(), world.getBlockState(feet.up()).getMaterial())
                || world.getBlockState(feet).getMaterial().isLiquid() || world.getBlockState(feet.up()).getMaterial().isLiquid()) {
            return false;
        }
        AxisAlignedBB box = new AxisAlignedBB(feet.getX() + 0.1D, feet.getY(), feet.getZ() + 0.1D,
                feet.getX() + 0.9D, feet.getY() + 1.8D, feet.getZ() + 0.9D);
        return world.getCollisionBoxes(player, box).isEmpty() && !world.containsAnyLiquid(box);
    }

    private static long horizontalSquared(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private static boolean isHazard(Block block, Material material) {
        return material == Material.LAVA || block == Blocks.LAVA || block == Blocks.FLOWING_LAVA
                || block == Blocks.FIRE || block == Blocks.MAGMA || block == Blocks.CACTUS;
    }

    private static UUID createUniqueStationId(TransportWorldState state) {
        UUID candidate;
        do {
            candidate = UUID.randomUUID();
        } while (state.getStation(candidate) != null);
        return candidate;
    }
}
