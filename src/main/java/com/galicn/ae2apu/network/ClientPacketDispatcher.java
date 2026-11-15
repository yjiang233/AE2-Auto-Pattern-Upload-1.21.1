package com.galicn.ae2apu.network;

import java.util.function.Consumer;

public final class ClientPacketDispatcher {
    private static volatile Consumer<ProvidersListPayload> handler;

    private ClientPacketDispatcher() {
    }

    public static void install(Consumer<ProvidersListPayload> consumer) {
        handler = consumer;
    }

    public static void dispatch(ProvidersListPayload payload) {
        Consumer<ProvidersListPayload> current = handler;
        if (current != null) {
            current.accept(payload);
        }
    }
}
