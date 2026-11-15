package com.galicn.ae2apu.network;

import com.galicn.ae2apu.AE2AutoPatternUpload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ScaleAmountsPayload(int factor, boolean divide) implements CustomPacketPayload {
    public static final Type<ScaleAmountsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AE2AutoPatternUpload.MODID, "scale_amounts"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScaleAmountsPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ScaleAmountsPayload decode(RegistryFriendlyByteBuf buf) {
                    return new ScaleAmountsPayload(buf.readVarInt(), buf.readBoolean());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, ScaleAmountsPayload payload) {
                    buf.writeVarInt(payload.factor());
                    buf.writeBoolean(payload.divide());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
