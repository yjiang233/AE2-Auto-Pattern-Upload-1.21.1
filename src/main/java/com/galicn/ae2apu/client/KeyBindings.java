package com.galicn.ae2apu.client;

import com.galicn.ae2apu.AE2AutoPatternUpload;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AE2AutoPatternUpload.MODID, value = Dist.CLIENT)
public final class KeyBindings {
    public static final KeyMapping SEARCH_ITEM = new KeyMapping(
            "key.ae2apu.search_from_viewer", GLFW.GLFW_KEY_F, "key.categories.ae2apu");

    private KeyBindings() {
    }

    @SubscribeEvent
    static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(SEARCH_ITEM);
    }
}
