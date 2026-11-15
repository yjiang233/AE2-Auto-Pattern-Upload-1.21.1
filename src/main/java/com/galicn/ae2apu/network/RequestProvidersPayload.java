package com.galicn.ae2apu.network;

import com.galicn.ae2apu.AE2AutoPatternUpload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestProvidersPayload(boolean direct) implements CustomPacketPayload {
    public static final Type<RequestProvidersPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AE2AutoPatternUpload.MODID, "request_providers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestProvidersPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public RequestProvidersPayload decode(RegistryFriendlyByteBuf buf) {
                    return new RequestProvidersPayload(buf.readBoolean());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, RequestProvidersPayload payload) {
                    buf.writeBoolean(payload.direct());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
