package com.galicn.ae2apu.client;

import com.galicn.ae2apu.network.ScaleAmountsPayload;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ScaleButton extends Button {
    private final int factor;
    private final boolean divide;

    public ScaleButton(int x, int y, int width, int height, int factor, boolean divide) {
        super(x, y, width, height,
                Component.literal((divide ? "÷" : "×") + factor),
                button -> PacketDistributor.sendToServer(new ScaleAmountsPayload(factor, divide)),
                DEFAULT_NARRATION);
        this.factor = factor;
        this.divide = divide;
        setTooltip(Tooltip.create(Component.translatable(
                divide ? "ae2apu.tip.divide" : "ae2apu.tip.multiply", factor)));
    }

    public int factor() {
        return factor;
    }

    public boolean divide() {
        return divide;
    }
}
