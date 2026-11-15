package com.galicn.ae2apu.client;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.patternaccess.PatternAccessTermScreen;
import appeng.client.gui.widgets.AETextField;
import appeng.integration.abstraction.ItemListMod;
import appeng.integration.abstraction.ItemListModAdapter;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class AeSearchWriter {
    private static final FallbackAdapter FALLBACK = new FallbackAdapter();
    private static boolean installed;

    private AeSearchWriter() {
    }

    public static void write(Screen screen, String text) {
        ensureAdapter();
        String value = text == null ? "" : text;
        ItemListMod.setSearchText("");
        pushJei("");
        setAeField(screen, "");
        ItemListMod.setSearchText(value);
        pushJei(value);
        setAeField(screen, value);
    }

    private static void ensureAdapter() {
        if (installed || ItemListMod.isEnabled()) {
            return;
        }
        ItemListMod.setAdapter(FALLBACK);
        installed = true;
    }

    private static void pushJei(String text) {
        if (!ModList.get().isLoaded("jei")) {
            return;
        }
        try {
            Class<?> adapter = Class.forName("com.galicn.ae2apu.client.jei.JeiAdapter");
            adapter.getMethod("setFilterText", String.class).invoke(null, text);
        } catch (Throwable ignored) {
        }
    }

    private static String pullJei() {
        if (!ModList.get().isLoaded("jei")) {
            return null;
        }
        try {
            Class<?> adapter = Class.forName("com.galicn.ae2apu.client.jei.JeiAdapter");
            Object result = adapter.getMethod("filterText").invoke(null);
            return result instanceof String text ? text : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void setAeField(Screen screen, String text) {
        AETextField field = findField(screen);
        if (field != null) {
            field.setValue(text);
        }
        if (screen instanceof MEStorageScreen<?>) {
            invoke(screen, MEStorageScreen.class, "setSearchText", new Class<?>[]{String.class}, text);
        } else if (screen instanceof PatternAccessTermScreen<?>) {
            invoke(screen, PatternAccessTermScreen.class, "refreshList", new Class<?>[0]);
        }
    }

    private static AETextField findField(Screen screen) {
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AETextField field) {
                return field;
            }
        }
        Class<?> type = screen.getClass();
        while (type != null && type != Object.class) {
            try {
                Field declared = type.getDeclaredField("searchField");
                declared.setAccessible(true);
                if (declared.get(screen) instanceof AETextField field) {
                    return field;
                }
            } catch (ReflectiveOperationException ignored) {
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static void invoke(Object target, Class<?> owner, String name, Class<?>[] params, Object... args) {
        try {
            Method method = owner.getDeclaredMethod(name, params);
            method.setAccessible(true);
            method.invoke(target, args);
        } catch (Throwable ignored) {
        }
    }

    private static final class FallbackAdapter implements ItemListModAdapter {
        private String text = "";

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public String getShortName() {
            return "JEI";
        }

        @Override
        public String getSearchText() {
            String jei = pullJei();
            return jei == null ? text : jei;
        }

        @Override
        public void setSearchText(String value) {
            text = value == null ? "" : value;
            pushJei(text);
        }

        @Override
        public boolean hasSearchFocus() {
            return false;
        }
    }
}
