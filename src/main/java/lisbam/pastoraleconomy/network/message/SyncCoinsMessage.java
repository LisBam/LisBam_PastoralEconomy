package lisbam.pastoraleconomy.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Minimal S2C display sync payload: the receiving player's long balance. */
public final class SyncCoinsMessage implements IMessage {
    private long balance;

    public SyncCoinsMessage() {
    }

    public SyncCoinsMessage(long balance) {
        this.balance = balance;
    }

    public long getBalance() {
        return balance;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        balance = buffer.readLong();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(balance);
    }
}
