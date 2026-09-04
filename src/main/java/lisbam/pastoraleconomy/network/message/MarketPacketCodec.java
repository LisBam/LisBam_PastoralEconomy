package lisbam.pastoraleconomy.network.message;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

/** Shared bounded UTF-8 codec for the two market-history packets. */
final class MarketPacketCodec {
    static final int MAX_COMMODITY_KEY_BYTES = 128;

    private MarketPacketCodec() {
    }

    static void writeCommodityKey(ByteBuf buffer, String key) {
        if (key == null) {
            throw new IllegalArgumentException("Market commodity key is required.");
        }
        byte[] encoded = key.getBytes(StandardCharsets.UTF_8);
        if (encoded.length == 0 || encoded.length > MAX_COMMODITY_KEY_BYTES) {
            throw new IllegalArgumentException("Market commodity key exceeds the packet limit.");
        }
        buffer.writeShort(encoded.length);
        buffer.writeBytes(encoded);
    }

    @Nullable
    static String readCommodityKey(ByteBuf buffer) {
        if (buffer.readableBytes() < 2) {
            return null;
        }
        int length = buffer.readUnsignedShort();
        if (length == 0 || length > MAX_COMMODITY_KEY_BYTES || buffer.readableBytes() < length) {
            return null;
        }
        byte[] encoded = new byte[length];
        buffer.readBytes(encoded);
        return new String(encoded, StandardCharsets.UTF_8);
    }
}
