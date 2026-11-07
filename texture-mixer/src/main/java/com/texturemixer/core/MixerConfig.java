package com.texturemixer.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Persistente Konfiguration (config/texturemixer/config.json):
 * Profile mit je einer unabhängigen Auswahl pro Kategorie, Favoriten und die Werte des Settings-Menüs.
 */
public final class MixerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class Profile {
        /** categoryId -> packId (kein Eintrag = Default, also kein Override) */
        public Map<String, String> selections = new LinkedHashMap<>();
    }

    public String activeProfile = "Survival";
    public Map<String, Profile> profiles = new LinkedHashMap<>();
    /** Einträge der Form "categoryId|packId" */
    public Set<String> favorites = new LinkedHashSet<>();
    public String lastCategory = "crystals";
    /** Werte der Einstellungen: Schlüssel "<feature>.enabled" bzw. "<feature>.<control>" -> Boolean oder Double */
    public Map<String, Object> settings = new LinkedHashMap<>();

    public void ensureDefaults() {
        if (profiles == null) profiles = new LinkedHashMap<>();
        if (favorites == null) favorites = new LinkedHashSet<>();
        if (settings == null) settings = new LinkedHashMap<>();
        if (profiles.isEmpty()) {
            profiles.put("Survival", new Profile());
            profiles.put("PvP", new Profile());
            profiles.put("Crystal PvP", new Profile());
        }
        if (activeProfile == null || !profiles.containsKey(activeProfile)) activeProfile = profiles.keySet().iterator().next();
        for (Profile p : profiles.values()) if (p.selections == null) p.selections = new LinkedHashMap<>();
    }

    public Profile active() { return profiles.get(activeProfile); }

    public boolean getBool(String key, boolean def) {
        Object v = settings.get(key);
        return v instanceof Boolean b ? b : def;
    }

    public double getNum(String key, double def) {
        Object v = settings.get(key);
        return v instanceof Number n ? n.doubleValue() : def;
    }

    public MixerConfig deepCopy() {
        MixerConfig c = GSON.fromJson(GSON.toJson(this), MixerConfig.class);
        c.ensureDefaults();
        return c;
    }

    public static MixerConfig load(Path file) {
        MixerConfig c = null;
        if (Files.isRegularFile(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                c = GSON.fromJson(r, MixerConfig.class);
            } catch (Exception e) {
                Log.warn("config.json defekt, starte mit Standardwerten", e);
            }
        }
        if (c == null) c = new MixerConfig();
        c.ensureDefaults();
        return c;
    }

    /** Atomar speichern (Temp-Datei + Move), damit die Config bei einem Crash nicht halb geschrieben wird. */
    public void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) { GSON.toJson(this, w); }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Log.warn("Config konnte nicht gespeichert werden", e);
        }
    }
}
