package com.galicn.ae2apu.client;

import com.galicn.ae2apu.network.ProvidersListPayload;
import net.minecraft.client.Minecraft;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void onProvidersList(ProvidersListPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ProviderSelectScreen screen) {
            screen.update(payload);
        } else {
            mc.setScreen(new ProviderSelectScreen(mc.screen, payload));
        }
    }
}
