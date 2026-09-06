package lisbam.pastoraleconomy.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Payload-free request; the server derives the player and equipped backpack. */
public final class RequestOpenBackpackMessage implements IMessage {
    private boolean valid = true;

    public boolean isValid() {
        return valid;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        valid = buffer.readableBytes() == 0;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (!valid) {
            throw new IllegalStateException("Cannot encode an invalid backpack request.");
        }
    }
}
