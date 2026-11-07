package com.texturemixer.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Ergänzt zu einer Dateiliste alles, worauf Modelle/Item-Definitionen im selben Pack verweisen
 * (Parent-Modelle, Texturen, .mcmeta). So funktionieren auch Packs mit eigenen Modellen/Textur-Namen.
 */
final class ModelDependencies {
    static Collection<String> expand(SourcePack pack, Collection<String> base) {
        Set<String> out = new LinkedHashSet<>(base);
        Deque<String> queue = new ArrayDeque<>();
        for (String f : base) if (isJsonModel(f)) queue.add(f);
        if (queue.isEmpty()) return out;
        try (SourcePack.PackReader r = pack.openReader()) {
            while (!queue.isEmpty()) {
                String path = queue.poll();
                byte[] data;
                try { data = r.read(path); } catch (Exception e) { continue; }
                if (data == null) continue;
                try {
                    JsonElement root = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
                    walk(root, pack, out, queue);
                } catch (Exception ignored) { /* kaputtes JSON im Pack ignorieren */ }
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static boolean isJsonModel(String f) {
        return f.endsWith(".json") && (f.contains("/models/") || f.contains("/items/"));
    }

    private static void walk(JsonElement e, SourcePack pack, Set<String> out, Deque<String> queue) {
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            for (Map.Entry<String, JsonElement> en : o.entrySet()) {
                String k = en.getKey();
                JsonElement v = en.getValue();
                if ((k.equals("model") || k.equals("parent")) && v.isJsonPrimitive()) {
                    addRef(v.getAsString(), "models/", ".json", pack, out, queue, true);
                } else if (k.equals("textures") && v.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> t : v.getAsJsonObject().entrySet()) {
                        if (t.getValue().isJsonPrimitive()) {
                            String s = t.getValue().getAsString();
                            if (!s.startsWith("#")) addRef(s, "textures/", ".png", pack, out, queue, false);
                        }
                    }
                } else {
                    walk(v, pack, out, queue);
                }
            }
        } else if (e.isJsonArray()) {
            for (JsonElement c : e.getAsJsonArray()) walk(c, pack, out, queue);
        }
    }

    private static void addRef(String ref, String dir, String ext, SourcePack pack, Set<String> out, Deque<String> queue, boolean isModel) {
        int i = ref.indexOf(':');
        String ns = i < 0 ? "minecraft" : ref.substring(0, i);
        String p = (i < 0 ? ref : ref.substring(i + 1)).toLowerCase();
        String path = "assets/" + ns.toLowerCase() + "/" + dir + p + ext;
        if (!pack.has(path) || out.contains(path)) return;
        out.add(path);
        if (isModel) queue.add(path);
        else if (pack.has(path + ".mcmeta")) out.add(path + ".mcmeta");
    }
}
