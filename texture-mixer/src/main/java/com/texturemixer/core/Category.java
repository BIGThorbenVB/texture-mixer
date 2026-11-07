package com.texturemixer.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Eine Textur-Kategorie (Crystals, Swords, ...). Datengetrieben über categories.json. */
public final class Category {
    public final String id;
    public final String name;
    public final String icon;
    public final List<String> previews;
    private final List<GlobRule> include = new ArrayList<>();
    private final List<GlobRule> exclude = new ArrayList<>();

    private Category(String id, String name, String icon, List<String> previews) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.previews = previews;
    }

    public static Category fromJson(JsonObject o) {
        String id = o.get("id").getAsString();
        String name = o.has("name") ? o.get("name").getAsString() : id;
        String icon = o.has("icon") ? o.get("icon").getAsString() : "minecraft:barrier";
        List<String> previews = new ArrayList<>();
        for (String p : strings(o, "preview")) previews.add(GlobRule.absolute(p));
        Category c = new Category(id, name, icon, previews);
        for (String item : strings(o, "items")) {
            c.include.add(GlobRule.of("textures/item/" + item + ".png"));
            c.include.add(GlobRule.of("models/item/" + item + ".json"));
            c.include.add(GlobRule.of("items/" + item + ".json"));
        }
        for (String f : strings(o, "files")) c.include.add(GlobRule.of(f));
        for (String f : strings(o, "exclude")) c.exclude.add(GlobRule.of(f));
        return c;
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key) && o.get(key).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(key);
            for (JsonElement e : a) out.add(e.getAsString());
        }
        return out;
    }

    public boolean matches(String path) {
        boolean hit = false;
        for (GlobRule r : include) if (r.matches(path)) { hit = true; break; }
        if (!hit) return false;
        for (GlobRule r : exclude) if (r.matches(path)) return false;
        return true;
    }
}
