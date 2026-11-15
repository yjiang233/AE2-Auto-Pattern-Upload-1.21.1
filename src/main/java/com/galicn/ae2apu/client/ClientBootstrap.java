package com.galicn.ae2apu.client;

import com.galicn.ae2apu.AE2AutoPatternUpload;
import com.galicn.ae2apu.network.ClientPacketDispatcher;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = AE2AutoPatternUpload.MODID, value = Dist.CLIENT)
public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ClientPacketDispatcher.install(ClientPayloadHandler::onProvidersList);
    }
}
