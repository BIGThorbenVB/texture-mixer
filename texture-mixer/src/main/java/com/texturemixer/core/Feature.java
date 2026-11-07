package com.texturemixer.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Eine Einstellung im Settings-Menü (datengetrieben aus features.json). */
public final class Feature {
    public enum Type { SLIDER, CYCLE }

    public static final class Control {
        public String key, nameEn, nameDe, descEn, descDe, unit = "";
        public Type type;
        public double min, max, step, def;
        public List<String> options = new ArrayList<>(), optionsDe = new ArrayList<>();
        public String name(boolean de) { return de ? nameDe : nameEn; }
        public String desc(boolean de) { return de ? descDe : descEn; }
        public String option(int i, boolean de) { List<String> l = de && !optionsDe.isEmpty() ? optionsDe : options; return l.get(Math.floorMod(i, l.size())); }
    }

    public String id, group, tab, icon, link, nameEn, nameDe, descEn, descDe;
    public boolean available, toggle, defaultOn;
    public final List<Control> controls = new ArrayList<>();

    public String name(boolean de) { return de ? nameDe : nameEn; }
    public String desc(boolean de) { return de ? descDe : descEn; }
    public String enabledKey() { return id + ".enabled"; }
    public String key(Control c) { return id + "." + c.key; }

    // ---- Zugriff auf die Werte
    public boolean enabled(MixerConfig cfg) { return available && (!toggle || cfg.getBool(enabledKey(), defaultOn)); }
    public double num(MixerConfig cfg, String control) { return cfg.getNum(id + "." + control, control(control).def); }
    public int index(MixerConfig cfg, String control) { return (int) Math.round(num(cfg, control)); }
    public Control control(String key) { for (Control c : controls) if (c.key.equals(key)) return c; throw new IllegalArgumentException(id + "." + key); }

    public void reset(MixerConfig cfg) {
        cfg.settings.remove(enabledKey());
        for (Control c : controls) cfg.settings.remove(key(c));
    }

    static Feature fromJson(JsonObject o) {
        Feature f = new Feature();
        f.id = o.get("id").getAsString();
        f.group = o.get("group").getAsString();
        f.tab = o.get("tab").getAsString();
        f.icon = o.get("icon").getAsString();
        f.available = o.get("available").getAsBoolean();
        f.toggle = o.get("toggle").getAsBoolean();
        f.defaultOn = o.get("default").getAsBoolean();
        f.link = o.get("link").isJsonNull() ? null : o.get("link").getAsString();
        f.nameEn = o.getAsJsonObject("name").get("en").getAsString();
        f.nameDe = o.getAsJsonObject("name").get("de").getAsString();
        f.descEn = o.getAsJsonObject("desc").get("en").getAsString();
        f.descDe = o.getAsJsonObject("desc").get("de").getAsString();
        for (JsonElement e : o.getAsJsonArray("controls")) {
            JsonObject c = e.getAsJsonObject();
            Control k = new Control();
            k.key = c.get("key").getAsString();
            k.type = c.get("type").getAsString().equals("slider") ? Type.SLIDER : Type.CYCLE;
            k.nameEn = c.getAsJsonObject("name").get("en").getAsString();
            k.nameDe = c.getAsJsonObject("name").get("de").getAsString();
            k.descEn = c.getAsJsonObject("desc").get("en").getAsString();
            k.descDe = c.getAsJsonObject("desc").get("de").getAsString();
            k.def = c.get("def").getAsDouble();
            if (c.has("min")) { k.min = c.get("min").getAsDouble(); k.max = c.get("max").getAsDouble(); k.step = c.get("step").getAsDouble(); }
            if (c.has("unit")) k.unit = c.get("unit").getAsString();
            if (c.has("options")) for (JsonElement x : c.getAsJsonArray("options")) k.options.add(x.getAsString());
            if (c.has("options_de")) for (JsonElement x : c.getAsJsonArray("options_de")) k.optionsDe.add(x.getAsString());
            f.controls.add(k);
        }
        return f;
    }
}
