package lisbam.pastoraleconomy.network.message;

import io.netty.buffer.ByteBuf;
import lisbam.pastoraleconomy.transport.TransportAction;
import lisbam.pastoraleconomy.transport.TransportNameRules;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Bounded C2S request containing only action, station UUID, and optional alias. */
public final class TransportStationActionMessage implements IMessage {
    private static final int MAX_ALIAS_BYTES = 96;

    private TransportAction action;
    private UUID stationId;
    private String alias = "";
    private boolean valid;

    public TransportStationActionMessage() {
    }

    public TransportStationActionMessage(TransportAction action, UUID stationId, String alias) {
        this.action = action;
        this.stationId = stationId;
        this.alias = alias == null ? "" : alias;
        valid = isValidPayload();
    }

    public boolean isValid() {
        return valid;
    }

    public TransportAction getAction() {
        return action;
    }

    public UUID getStationId() {
        return stationId;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        valid = false;
        alias = "";
        if (buffer.readableBytes() < 1 + 16 + 2) {
            return;
        }
        int ordinal = buffer.readUnsignedByte();
        if (ordinal < 0 || ordinal >= TransportAction.values().length) {
            return;
        }
        stationId = new UUID(buffer.readLong(), buffer.readLong());
        int length = buffer.readUnsignedShort();
        if (length > MAX_ALIAS_BYTES || buffer.readableBytes() < length) {
            return;
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        action = TransportAction.values()[ordinal];
        alias = new String(bytes, StandardCharsets.UTF_8);
        valid = isValidPayload();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (!valid) {
            throw new IllegalStateException("Cannot encode invalid transport action.");
        }
        byte[] bytes = alias.getBytes(StandardCharsets.UTF_8);
        buffer.writeByte(action.ordinal());
        buffer.writeLong(stationId.getMostSignificantBits());
        buffer.writeLong(stationId.getLeastSignificantBits());
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);
    }

    private boolean isValidPayload() {
        if (action == null || stationId == null) {
            return false;
        }
        byte[] bytes = alias.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_ALIAS_BYTES) {
            return false;
        }
        return action == TransportAction.RENAME ? TransportNameRules.isValidAlias(alias) : alias.isEmpty();
    }
}
