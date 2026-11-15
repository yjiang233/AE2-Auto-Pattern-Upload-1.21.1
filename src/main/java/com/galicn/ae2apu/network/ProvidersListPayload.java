package com.galicn.ae2apu.network;

import com.galicn.ae2apu.AE2AutoPatternUpload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ProvidersListPayload(List<Entry> providers, String productId, String method, int defaultIndex) implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 512;
    public static final int MAX_TEXT = 256;

    public record Entry(int index, String name, int emptySlots) {
    }

    public static final Type<ProvidersListPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AE2AutoPatternUpload.MODID, "providers_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProvidersListPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ProvidersListPayload decode(RegistryFriendlyByteBuf buf) {
                    int size = Math.min(buf.readVarInt(), MAX_ENTRIES);
                    List<Entry> entries = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        entries.add(new Entry(buf.readVarInt(), buf.readUtf(MAX_TEXT), buf.readVarInt()));
                    }
                    return new ProvidersListPayload(entries, buf.readUtf(MAX_TEXT),
                            buf.readUtf(MAX_TEXT), buf.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, ProvidersListPayload payload) {
                    List<Entry> entries = payload.providers();
                    int size = Math.min(entries.size(), MAX_ENTRIES);
                    buf.writeVarInt(size);
                    for (int i = 0; i < size; i++) {
                        Entry entry = entries.get(i);
                        buf.writeVarInt(entry.index());
                        buf.writeUtf(truncate(entry.name()), MAX_TEXT);
                        buf.writeVarInt(entry.emptySlots());
                    }
                    buf.writeUtf(truncate(payload.productId()), MAX_TEXT);
                    buf.writeUtf(truncate(payload.method()), MAX_TEXT);
                    buf.writeVarInt(payload.defaultIndex());
                }
            };

    public ProvidersListPayload {
        providers = List.copyOf(providers);
        productId = productId == null ? "" : productId;
        method = method == null ? "" : method;
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > MAX_TEXT ? value.substring(0, MAX_TEXT) : value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
