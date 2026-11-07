package com.texturemixer.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FeatureRegistry {
    private static final Map<String, Feature> BY_ID = new LinkedHashMap<>();
    static {
        try (InputStream in = FeatureRegistry.class.getResourceAsStream("/assets/texturemixer/features.json")) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement e : root.getAsJsonArray("features")) { Feature f = Feature.fromJson(e.getAsJsonObject()); BY_ID.put(f.id, f); }
        } catch (Exception e) { Log.warn("features.json konnte nicht geladen werden", e); }
    }
    private FeatureRegistry() {}

    public static List<Feature> all() { return new ArrayList<>(BY_ID.values()); }
    public static Feature get(String id) { return BY_ID.get(id); }
    public static List<Feature> tab(String tab) { List<Feature> l = new ArrayList<>(); for (Feature f : BY_ID.values()) if (f.tab.equals(tab)) l.add(f); return l; }
}
