package com.galicn.ae2apu.client;

import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

public final class RecipeCategoryResolver {
    private RecipeCategoryResolver() {
    }

    public static String resolve(String productId, String method) {
        String category = null;
        if (ModList.get().isLoaded("jei")) {
            try {
                Class<?> adapter = Class.forName("com.galicn.ae2apu.client.jei.JeiAdapter");
                Object result = adapter.getMethod("categoryName", String.class, String.class)
                        .invoke(null, productId, method);
                if (result instanceof String text && !text.isBlank()) {
                    category = text;
                }
            } catch (Throwable ignored) {
            }
        }
        if (category == null) {
            category = fallback(method);
        }
        return RecipeNameMappings.map(category);
    }

    private static String fallback(String method) {
        String key = method == null || method.isBlank() ? "crafting" : method;
        return Component.translatable("ae2apu.method." + key).getString();
    }
}
