package lisbam.pastoraleconomy.network.message;

import io.netty.buffer.ByteBuf;
import lisbam.pastoraleconomy.transport.TransportNodeView;
import lisbam.pastoraleconomy.transport.TransportStationType;
import lisbam.pastoraleconomy.transport.TransportStateSnapshot;
import lisbam.pastoraleconomy.transport.VillageTransportCandidate;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bounded server-authoritative snapshot for one transport management GUI. */
public final class SyncTransportStateMessage implements IMessage {
    private static final int MAX_ALIAS_BYTES = 96;
    private static final int MAX_NODES = 2048;

    private TransportStateSnapshot snapshot;

    public SyncTransportStateMessage() {
    }

    public SyncTransportStateMessage(TransportStateSnapshot snapshot) {
        if (snapshot == null || snapshot.getNodes().size() > MAX_NODES) {
            throw new IllegalArgumentException("Transport snapshot is absent or too large.");
        }
        this.snapshot = snapshot;
    }

    public TransportStateSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        snapshot = null;
        if (buffer.readableBytes() < 1) {
            return;
        }
        UUID currentStationId = null;
        boolean hasCurrentStation = buffer.readBoolean();
        if (hasCurrentStation) {
            if (buffer.readableBytes() < 16) {
                return;
            }
            currentStationId = new UUID(buffer.readLong(), buffer.readLong());
        }
        String currentAlias = readString(buffer);
        if (currentAlias == null || buffer.readableBytes() < 4 + 16 + 8 + 8 + 2) {
            return;
        }
        boolean currentExists = buffer.readBoolean();
        boolean supported = buffer.readBoolean();
        boolean active = buffer.readBoolean();
        boolean starterFree = buffer.readBoolean();
        int dimension = buffer.readInt();
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        long coins = buffer.readLong();
        long fee = buffer.readLong();
        VillageTransportCandidate nearestVillage = readVillageCandidate(buffer);
        if (nearestVillage == INVALID_CANDIDATE) {
            return;
        }
        int count = buffer.readUnsignedShort();
        if (coins < 0L || fee < 0L || count > MAX_NODES) {
            return;
        }
        List<TransportNodeView> nodes = new ArrayList<TransportNodeView>(count);
        for (int index = 0; index < count; index++) {
            TransportNodeView node = readNode(buffer);
            if (node == null) {
                return;
            }
            nodes.add(node);
        }
        snapshot = new TransportStateSnapshot(currentStationId, currentAlias, currentExists, supported, active,
                starterFree, dimension, x, y, z, coins, fee, nodes, nearestVillage);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (snapshot == null || snapshot.getNodes().size() > MAX_NODES) {
            throw new IllegalStateException("Cannot encode absent or oversized transport snapshot.");
        }
        buffer.writeBoolean(snapshot.hasCurrentStation());
        if (snapshot.hasCurrentStation()) {
            writeUuid(buffer, snapshot.getCurrentStationId());
        }
        writeString(buffer, snapshot.getCurrentAlias());
        buffer.writeBoolean(snapshot.currentStationExists());
        buffer.writeBoolean(snapshot.isCurrentDimensionSupported());
        buffer.writeBoolean(snapshot.isCurrentActive());
        buffer.writeBoolean(snapshot.isStarterFree());
        buffer.writeInt(snapshot.getDimension());
        buffer.writeInt(snapshot.getX());
        buffer.writeInt(snapshot.getY());
        buffer.writeInt(snapshot.getZ());
        buffer.writeLong(snapshot.getCoins());
        buffer.writeLong(snapshot.getConnectionFee());
        writeVillageCandidate(buffer, snapshot.getNearestVillage());
        buffer.writeShort(snapshot.getNodes().size());
        for (TransportNodeView node : snapshot.getNodes()) {
            writeNode(buffer, node);
        }
    }

    private static TransportNodeView readNode(ByteBuf buffer) {
        if (buffer.readableBytes() < 16 + 2) {
            return null;
        }
        UUID id = new UUID(buffer.readLong(), buffer.readLong());
        boolean active = buffer.readBoolean();
        boolean exists = buffer.readBoolean();
        String alias = readString(buffer);
        if (alias == null || buffer.readableBytes() < 4 * 4 + 1 + 8) {
            return null;
        }
        int dimension = buffer.readInt();
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        int typeOrdinal = buffer.readUnsignedByte();
        long travelFee = buffer.readLong();
        if (typeOrdinal >= TransportStationType.values().length || travelFee < -1L) {
            return null;
        }
        return new TransportNodeView(id, active, alias, exists, dimension, x, y, z,
                TransportStationType.values()[typeOrdinal], travelFee);
    }

    private static void writeNode(ByteBuf buffer, TransportNodeView node) {
        writeUuid(buffer, node.getStationId());
        buffer.writeBoolean(node.isActive());
        buffer.writeBoolean(node.exists());
        writeString(buffer, node.getAlias());
        buffer.writeInt(node.getDimension());
        buffer.writeInt(node.getX());
        buffer.writeInt(node.getY());
        buffer.writeInt(node.getZ());
        buffer.writeByte(node.getType().ordinal());
        buffer.writeLong(node.getTravelFee());
    }

    private static String readString(ByteBuf buffer) {
        if (buffer.readableBytes() < 2) {
            return null;
        }
        int length = buffer.readUnsignedShort();
        if (length > MAX_ALIAS_BYTES || buffer.readableBytes() < length) {
            return null;
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeString(ByteBuf buffer, String value) {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_ALIAS_BYTES) {
            throw new IllegalArgumentException("Transport alias exceeds packet limit.");
        }
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);
    }

    private static void writeUuid(ByteBuf buffer, UUID value) {
        buffer.writeLong(value.getMostSignificantBits());
        buffer.writeLong(value.getLeastSignificantBits());
    }

    private static final VillageTransportCandidate INVALID_CANDIDATE = new VillageTransportCandidate(
            new UUID(0L, 1L), null, new net.minecraft.util.math.BlockPos(0, 0, 0), 0L, 0L, "");

    private static VillageTransportCandidate readVillageCandidate(ByteBuf buffer) {
        if (buffer.readableBytes() < 1) {
            return INVALID_CANDIDATE;
        }
        boolean present = buffer.readBoolean();
        if (!present) {
            return null;
        }
        if (buffer.readableBytes() < 16 + 1 + 12 + 8 + 8 + 2) {
            return INVALID_CANDIDATE;
        }
        UUID villageId = new UUID(buffer.readLong(), buffer.readLong());
        boolean hasStation = buffer.readBoolean();
        UUID stationId = hasStation ? new UUID(buffer.readLong(), buffer.readLong()) : null;
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        long distance = buffer.readLong();
        long fee = buffer.readLong();
        String name = readString(buffer);
        if (name == null || distance < 0L || fee < 0L) {
            return INVALID_CANDIDATE;
        }
        return new VillageTransportCandidate(villageId, stationId, new net.minecraft.util.math.BlockPos(x, y, z),
                distance, fee, name);
    }

    private static void writeVillageCandidate(ByteBuf buffer, VillageTransportCandidate candidate) {
        buffer.writeBoolean(candidate != null);
        if (candidate == null) {
            return;
        }
        writeUuid(buffer, candidate.getVillageId());
        buffer.writeBoolean(candidate.getStationId() != null);
        if (candidate.getStationId() != null) {
            writeUuid(buffer, candidate.getStationId());
        }
        buffer.writeInt(candidate.getPosition().getX());
        buffer.writeInt(candidate.getPosition().getY());
        buffer.writeInt(candidate.getPosition().getZ());
        buffer.writeLong(candidate.getDistance());
        buffer.writeLong(candidate.getConnectionFee());
        writeString(buffer, candidate.getDisplayName());
    }
}
