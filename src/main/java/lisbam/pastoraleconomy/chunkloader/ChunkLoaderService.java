package lisbam.pastoraleconomy.chunkloader;

import lisbam.pastoraleconomy.LisBamPastoralEconomy;
import lisbam.pastoraleconomy.block.BlockChunkLoader;
import lisbam.pastoraleconomy.block.ModBlocks;
import lisbam.pastoraleconomy.tile.TileChunkLoader;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.FMLLog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Server-only lifecycle for the chunk-loader Forge tickets. */
public final class ChunkLoaderService {
    private static final String TICKET_KIND_KEY = "lisbamTicketKind";
    private static final String TICKET_KIND = "chunk_loader";
    private static final String TICKET_POSITION_KEY = "chunkLoaderPosition";
    private static final Map<Location, ForgeChunkManager.Ticket> LOADER_TICKETS =
            new HashMap<Location, ForgeChunkManager.Ticket>();

    private ChunkLoaderService() {
    }

    public static void onChunkLoaderTileLoaded(TileChunkLoader tile) {
        if (tile != null && tile.getWorld() != null) {
            updateForRedstone(tile.getWorld(), tile.getPos(), tile);
        }
    }

    public static void updateForRedstone(World world, BlockPos pos, TileChunkLoader tile) {
        if (!(world instanceof WorldServer) || world.isRemote || tile == null || tile.getWorld() != world
                || !pos.equals(tile.getPos()) || world.getBlockState(pos).getBlock() != ModBlocks.CHUNK_LOADER) {
            return;
        }
        if (ChunkLoaderRules.shouldBeActive(world.isBlockPowered(pos))) {
            activate((WorldServer) world, pos, tile);
        } else {
            deactivate((WorldServer) world, pos, tile);
        }
    }

    public static void onChunkLoaderBroken(World world, BlockPos pos, TileChunkLoader tile) {
        if (world instanceof WorldServer && !world.isRemote) {
            deactivate((WorldServer) world, pos, tile);
        }
    }

    /**
     * Called by the single mod-wide Forge ticket callback. Returns true only
     * when the ticket was recognized as a chunk-loader ticket and consumed.
     */
    public static boolean restoreChunkLoaderTicket(ForgeChunkManager.Ticket ticket, World world) {
        if (ticket == null || world == null || !TICKET_KIND.equals(ticket.getModData().getString(TICKET_KIND_KEY))) {
            return false;
        }
        if (!(world instanceof WorldServer) || !ticket.getModData().hasKey(TICKET_POSITION_KEY)) {
            ForgeChunkManager.releaseTicket(ticket);
            return true;
        }

        Location location = new Location(world.provider.getDimension(),
                ticket.getModData().getLong(TICKET_POSITION_KEY));
        ForgeChunkManager.Ticket oldTicket = LOADER_TICKETS.get(location);
        if (oldTicket != null && oldTicket != ticket) {
            ForgeChunkManager.releaseTicket(ticket);
            return true;
        }
        LOADER_TICKETS.put(location, ticket);
        ForgeChunkManager.forceChunk(ticket, new ChunkPos(location.getPosition()));
        return true;
    }

    /** Remove runtime references only; Forge writes valid tickets during normal world unload. */
    public static void forgetWorld(World world) {
        if (world == null) {
            return;
        }
        Iterator<Map.Entry<Location, ForgeChunkManager.Ticket>> iterator = LOADER_TICKETS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().world == world) {
                iterator.remove();
            }
        }
    }

    private static void activate(WorldServer world, BlockPos pos, TileChunkLoader tile) {
        Location location = new Location(world.provider.getDimension(), pos.toLong());
        ForgeChunkManager.Ticket ticket = LOADER_TICKETS.get(location);
        if (ticket != null && ticket.world == world) {
            ForgeChunkManager.forceChunk(ticket, new ChunkPos(pos));
            setActiveState(world, pos, tile, true);
            return;
        }
        if (ticket != null) {
            LOADER_TICKETS.remove(location);
            ForgeChunkManager.releaseTicket(ticket);
        }

        ticket = ForgeChunkManager.requestTicket(LisBamPastoralEconomy.INSTANCE, world, ForgeChunkManager.Type.NORMAL);
        if (ticket == null) {
            FMLLog.log.warn("Chunk loader at {} could not obtain a Forge chunk ticket.", pos);
            setActiveState(world, pos, tile, false);
            return;
        }
        NBTTagCompound ticketData = ticket.getModData();
        ticketData.setString(TICKET_KIND_KEY, TICKET_KIND);
        ticketData.setLong(TICKET_POSITION_KEY, pos.toLong());
        LOADER_TICKETS.put(location, ticket);
        ForgeChunkManager.forceChunk(ticket, new ChunkPos(pos));
        setActiveState(world, pos, tile, true);
    }

    private static void deactivate(WorldServer world, BlockPos pos, TileChunkLoader tile) {
        Location location = new Location(world.provider.getDimension(), pos.toLong());
        ForgeChunkManager.Ticket ticket = LOADER_TICKETS.remove(location);
        if (ticket != null) {
            ForgeChunkManager.releaseTicket(ticket);
        }
        setActiveState(world, pos, tile, false);
    }

    private static void setActiveState(WorldServer world, BlockPos pos, TileChunkLoader tile, boolean active) {
        if (tile != null) {
            tile.setActive(active);
        }
        IBlockState current = world.getBlockState(pos);
        if (current.getBlock() == ModBlocks.CHUNK_LOADER
                && current.getValue(BlockChunkLoader.POWERED).booleanValue() != active) {
            world.setBlockState(pos, current.withProperty(BlockChunkLoader.POWERED, Boolean.valueOf(active)), 2);
        }
    }

    private static final class Location {
        private final int dimension;
        private final long packedPosition;

        private Location(int dimension, long packedPosition) {
            this.dimension = dimension;
            this.packedPosition = packedPosition;
        }

        private BlockPos getPosition() {
            return BlockPos.fromLong(packedPosition);
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
