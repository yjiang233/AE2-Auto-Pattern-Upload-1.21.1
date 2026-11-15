package com.galicn.ae2apu.network;

import com.galicn.ae2apu.AE2AutoPatternUpload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UploadPatternPayload(int providerIndex) implements CustomPacketPayload {
    public static final Type<UploadPatternPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AE2AutoPatternUpload.MODID, "upload_pattern"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UploadPatternPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UploadPatternPayload decode(RegistryFriendlyByteBuf buf) {
                    return new UploadPatternPayload(buf.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, UploadPatternPayload payload) {
                    buf.writeVarInt(payload.providerIndex());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
