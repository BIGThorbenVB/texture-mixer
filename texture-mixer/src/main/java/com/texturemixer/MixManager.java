package com.texturemixer;

import com.texturemixer.core.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.List;

/** Zentrale Verwaltung: Config, Pack-Bibliothek, Kategorien und das aktive kombinierte Pack. */
public final class MixManager {
    private static MixerConfig config;
    private static Path configFile;
    private static PackLibrary library;
    private static CategoryRegistry categories;
    private static final StyleResolver STYLES = new StyleResolver();
    private static volatile ActiveMix mix;
    private static volatile int appliedSignature = Integer.MIN_VALUE;

    private MixManager() {}

    public static synchronized void init() {
        if (config != null) return;
        Path cfgDir = FabricLoader.getInstance().getConfigDir().resolve("texturemixer");
        configFile = cfgDir.resolve("config.json");
        config = MixerConfig.load(configFile);
        library = new PackLibrary(FabricLoader.getInstance().getGameDir().resolve("resourcepacks"));
        categories = CategoryRegistry.load(cfgDir.resolve("categories"));
    }

    public static MixerConfig config() { init(); return config; }
    public static PackLibrary library() { init(); return library; }
    public static List<Category> categories() { init(); return categories.all(); }
    public static StyleResolver styles() { return STYLES; }
    public static void saveConfig() { init(); config.save(configFile); }

    /** Wird vom Pack-Source beim (Neu-)Laden der Resource Packs aufgerufen. */
    public static synchronized ActiveMix currentMix() {
        init();
        if (mix == null) { mix = build(); appliedSignature = mix.signature(); }
        return mix;
    }

    private static ActiveMix build() {
        return MixBuilder.build(config.active().selections, categories.all(), library, STYLES);
    }

    /** Übernimmt Profile/Favoriten aus einer Arbeitskopie (Einstellungen bleiben unangetastet) und speichert. */
    public static synchronized void commit(MixerConfig draft) {
        init();
        config.profiles = draft.profiles;
        config.activeProfile = draft.activeProfile;
        config.favorites = draft.favorites;
        config.lastCategory = draft.lastCategory;
        config.ensureDefaults();
        saveConfig();
    }

    /** Baut das Pack neu und lädt die Resource Packs neu, falls sich die Auswahl geändert hat. */
    public static synchronized boolean applyIfChanged() {
        init();
        ActiveMix next = build();
        currentMix();
        if (next.signature() == appliedSignature) return false;
        ActiveMix old = mix;
        mix = next;
        appliedSignature = next.signature();
        Minecraft mc = Minecraft.getInstance();
        mc.getResourcePackRepository().reload();   // fragt VirtualPackSource erneut ab
        mc.reloadResourcePacks().thenRun(old::close);
        return true;
    }

    /** Verwirft gecachte Pack-Infos (z. B. nach Hinzufügen neuer Packs). */
    public static synchronized void rescan() { init(); library.rescan(); STYLES.clear(); }
}
