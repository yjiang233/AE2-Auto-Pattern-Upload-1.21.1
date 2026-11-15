package com.galicn.ae2apu.network;

import com.galicn.ae2apu.AE2AutoPatternUpload;
import com.galicn.ae2apu.server.PatternUploadService;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.server.level.ServerPlayer;

public final class ApuNetwork {
    private ApuNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(AE2AutoPatternUpload.MODID).versioned("1");

        registrar.playToServer(RequestProvidersPayload.TYPE, RequestProvidersPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        context.enqueueWork(() -> PatternUploadService.handleRequest(player, payload.direct()));
                    }
                });

        registrar.playToServer(UploadPatternPayload.TYPE, UploadPatternPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        context.enqueueWork(() -> PatternUploadService.handleUpload(player, payload.providerIndex()));
                    }
                });

        registrar.playToServer(ScaleAmountsPayload.TYPE, ScaleAmountsPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        context.enqueueWork(() -> PatternUploadService.handleScale(player, payload.factor(), payload.divide()));
                    }
                });

        registrar.playToClient(ProvidersListPayload.TYPE, ProvidersListPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketDispatcher.dispatch(payload)));
    }
}
