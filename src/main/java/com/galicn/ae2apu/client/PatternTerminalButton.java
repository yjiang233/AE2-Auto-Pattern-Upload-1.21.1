package com.galicn.ae2apu.client;

import appeng.client.gui.AEBaseScreen;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.galicn.ae2apu.AE2AutoPatternUpload;
import com.galicn.ae2apu.network.RequestProvidersPayload;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AE2AutoPatternUpload.MODID, value = Dist.CLIENT)
public final class PatternTerminalButton {
    private static final int ENCODE_BUTTON_X = 147;
    private static final int ENCODE_BUTTON_BOTTOM = 145;
    private static final int RESULT_BOTTOM = 118;
    private static final int BUTTON_SIZE = 14;
    private static final int SCALE_WIDTH = 22;
    private static final int SCALE_HEIGHT = 13;
    private static final int[] FACTORS = {2, 3, 5};

    private PatternTerminalButton() {
    }

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AEBaseScreen<?> screen)) {
            return;
        }
        if (!(screen.getMenu() instanceof PatternEncodingTermMenu)) {
            return;
        }

        int guiLeft = screen.getGuiLeft();
        int guiTop = screen.getGuiTop();
        int imageHeight = screen.height - 2 * guiTop;
        int imageWidth = screen.width - 2 * guiLeft;

        Button upload = Button.builder(Component.literal("↑"), button ->
                        PacketDistributor.sendToServer(new RequestProvidersPayload(Screen.hasShiftDown())))
                .bounds(guiLeft + ENCODE_BUTTON_X - BUTTON_SIZE - 2,
                        guiTop + imageHeight - ENCODE_BUTTON_BOTTOM + 2,
                        BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("ae2apu.button.upload")))
                .build();
        event.addListener(upload);

        int baseY = guiTop + imageHeight - RESULT_BOTTOM + 2;
        int leftX = guiLeft - SCALE_WIDTH - 3;
        int rightX = guiLeft + imageWidth + 3;
        for (int i = 0; i < FACTORS.length; i++) {
            event.addListener(new ScaleButton(leftX, baseY + i * (SCALE_HEIGHT + 1),
                    SCALE_WIDTH, SCALE_HEIGHT, FACTORS[i], true));
            event.addListener(new ScaleButton(rightX, baseY + i * (SCALE_HEIGHT + 1),
                    SCALE_WIDTH, SCALE_HEIGHT, FACTORS[i], false));
        }
    }
}
