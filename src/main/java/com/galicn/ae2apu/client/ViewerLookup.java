package com.galicn.ae2apu.client;

import appeng.client.gui.AEBaseScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.List;
import java.util.Optional;

public final class ViewerLookup {
    private ViewerLookup() {
    }

    public static String hoveredName() {
        String name = clean(jeiName());
        if (name != null) {
            return name;
        }
        name = clean(emiName());
        if (name != null) {
            return name;
        }
        name = clean(reiName());
        if (name != null) {
            return name;
        }
        return clean(hoveredSlotName());
    }

    private static String jeiName() {
        if (!ModList.get().isLoaded("jei")) {
            return null;
        }
        try {
            Class<?> adapter = Class.forName("com.galicn.ae2apu.client.jei.JeiAdapter");
            Object result = adapter.getMethod("hoveredName").invoke(null);
            return result instanceof String text ? text : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String emiName() {
        try {
            Class<?> api = Class.forName("dev.emi.emi.api.EmiApi");
            Object interaction = api.getMethod("getHoveredStack", boolean.class).invoke(null, true);
            if (interaction == null) {
                return null;
            }
            Object ingredient = interaction.getClass().getMethod("getStack").invoke(interaction);
            if (ingredient == null) {
                return null;
            }
            Object stacks = ingredient.getClass().getMethod("getEmiStacks").invoke(ingredient);
            if (stacks instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                Object component = first.getClass().getMethod("getName").invoke(first);
                return component instanceof Component text ? text.getString() : null;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String reiName() {
        try {
            Class<?> runtimeClass = Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            Object runtime = runtimeClass.getMethod("getInstance").invoke(null);
            Object overlayOptional = runtimeClass.getMethod("getOverlay").invoke(runtime);
            if (!(overlayOptional instanceof Optional<?> optional) || optional.isEmpty()) {
                return null;
            }
            Object overlay = optional.get();
            Object list = overlay.getClass().getMethod("getEntryList").invoke(overlay);
            if (list == null) {
                return null;
            }
            Object entry = list.getClass().getMethod("getFocusedStack").invoke(list);
            if (entry == null || Boolean.TRUE.equals(entry.getClass().getMethod("isEmpty").invoke(entry))) {
                return null;
            }
            Object value = entry.getClass().getMethod("getValue").invoke(entry);
            if (value instanceof ItemStack stack && !stack.isEmpty()) {
                return stack.getHoverName().getString();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String hoveredSlotName() {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof AEBaseScreen<?> ae) {
            Slot slot = ae.getSlotUnderMouse();
            if (slot != null && slot.hasItem()) {
                return slot.getItem().getHoverName().getString();
            }
        }
        return null;
    }

    private static String clean(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
        return text.isEmpty() ? null : text;
    }
}
