package lisbam.pastoraleconomy.gui;

import lisbam.pastoraleconomy.block.ModBlocks;
import lisbam.pastoraleconomy.tile.TileTransportStation;
import lisbam.pastoraleconomy.tile.TileVillageStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** Slotless server container binding a transport GUI to its physically present node. */
public final class ContainerTransportStation extends Container {
    private final UUID stationId;
    private final BlockPos position;

    public ContainerTransportStation(TileTransportStation tile) {
        if (tile == null || tile.getStationId() == null || tile.getPos() == null) {
            throw new IllegalArgumentException("Transport station container needs a bound tile.");
        }
        stationId = tile.getStationId();
        position = tile.getPos().toImmutable();
    }

    public ContainerTransportStation(TileVillageStation tile) {
        if (tile == null || tile.getStationId() == null || tile.getPos() == null || !tile.isVillageStation()) {
            throw new IllegalArgumentException("Village station container needs a bound tile.");
        }
        stationId = tile.getStationId();
        position = tile.getPos().toImmutable();
    }

    /**
     * Client-side lifecycle counterpart for Forge's OpenGui protocol. The
     * authoritative server always uses one of the tile-bound constructors.
     */
    public ContainerTransportStation(BlockPos position) {
        if (position == null) {
            throw new IllegalArgumentException("Transport station client container needs a position.");
        }
        stationId = null;
        this.position = position.toImmutable();
    }

    public UUID getStationId() {
        return stationId;
    }

    public BlockPos getPosition() {
        return position;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (stationId == null) {
            return false;
        }
        TileEntity tile = player.world.getTileEntity(position);
        boolean identity = tile instanceof TileTransportStation
                ? stationId.equals(((TileTransportStation) tile).getStationId())
                : tile instanceof TileVillageStation && ((TileVillageStation) tile).isVillageStation()
                && stationId.equals(((TileVillageStation) tile).getStationId());
        boolean block = player.world.getBlockState(position).getBlock() == ModBlocks.TRANSPORT_STATION
                || player.world.getBlockState(position).getBlock() == ModBlocks.VILLAGE_STATION;
        return identity && block && player.getDistanceSq(position) <= 64.0D;
    }
}
