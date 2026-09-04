package lisbam.pastoraleconomy.client;

import lisbam.pastoraleconomy.transport.TransportStateSnapshot;

import javax.annotation.Nullable;

/** Non-persistent transport GUI cache. It never authorizes a client action. */
public final class ClientTransportState {
    private static TransportStateSnapshot snapshot;

    private ClientTransportState() {
    }

    public static void accept(TransportStateSnapshot value) {
        snapshot = value;
    }

    @Nullable
    public static TransportStateSnapshot get() {
        return snapshot;
    }

    public static void clear() {
        snapshot = null;
    }
}
