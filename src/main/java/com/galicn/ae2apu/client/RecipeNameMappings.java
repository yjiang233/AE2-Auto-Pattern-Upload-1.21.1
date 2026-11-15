package com.galicn.ae2apu.client;

import com.galicn.ae2apu.AE2AutoPatternUpload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class RecipeNameMappings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("ae2apu").resolve("recipe_names.json");
    private static final Map<String, String> MAPPINGS = new LinkedHashMap<>();

    static {
        reload();
    }

    private RecipeNameMappings() {
    }

    public static String map(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        String mapped = MAPPINGS.get(trimmed.toLowerCase(Locale.ROOT));
        return mapped == null || mapped.isBlank() ? trimmed : mapped;
    }

    public static boolean add(String key, String value) {
        if (key == null || value == null || key.isBlank() || value.isBlank()) {
            return false;
        }
        MAPPINGS.put(key.trim().toLowerCase(Locale.ROOT), value.trim());
        return save();
    }

    public static int removeByValue(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        int removed = 0;
        var iterator = MAPPINGS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().equalsIgnoreCase(value.trim())) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            save();
        }
        return removed;
    }

    public static void reload() {
        MAPPINGS.clear();
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                return;
            }
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    MAPPINGS.put(entry.getKey().trim().toLowerCase(Locale.ROOT), entry.getValue().getAsString().trim());
                }
            }
        } catch (IOException | RuntimeException e) {
            AE2AutoPatternUpload.LOG.warn("读取 {} 失败", FILE, e);
        }
    }

    private static boolean save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject json = new JsonObject();
            MAPPINGS.forEach(json::addProperty);
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            AE2AutoPatternUpload.LOG.warn("写入 {} 失败", FILE, e);
            return false;
        }
    }
}
