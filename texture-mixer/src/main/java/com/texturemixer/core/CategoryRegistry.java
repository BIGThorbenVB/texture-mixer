package com.texturemixer.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Eingebaute Kategorien (assets/texturemixer/categories.json) plus eigene aus config/texturemixer/categories/*.json.
 * Eigene Dateien haben dasselbe Format; gleiche IDs überschreiben die eingebauten, neue IDs werden angehängt.
 */
public final class CategoryRegistry {
    private final Map<String, Category> map = new LinkedHashMap<>();

    public static CategoryRegistry load(Path userDir) {
        CategoryRegistry reg = new CategoryRegistry();
        try (InputStream in = CategoryRegistry.class.getResourceAsStream("/assets/texturemixer/categories.json")) {
            if (in != null) reg.readInto(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.warn("Eingebaute Kategorien konnten nicht geladen werden", e);
        }
        if (userDir != null && Files.isDirectory(userDir)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(userDir, "*.json")) {
                for (Path p : ds) {
                    try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                        reg.readInto(r);
                    } catch (Exception e) {
                        Log.warn("Ungültige Kategorie-Datei " + p.getFileName(), e);
                    }
                }
            } catch (Exception ignored) { }
        }
        return reg;
    }

    private void readInto(Reader r) {
        JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
        for (JsonElement e : root.getAsJsonArray("categories")) {
            Category c = Category.fromJson(e.getAsJsonObject());
            map.put(c.id, c);
        }
    }

    public List<Category> all() { return new ArrayList<>(map.values()); }
    public Category get(String id) { return map.get(id); }
}
