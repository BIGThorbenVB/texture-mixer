package com.texturemixer.core;

import java.util.*;

/** Berechnet (und cached) pro Pack+Kategorie, welche Dateien dazugehören. */
public final class StyleResolver {
    private final Map<String, Optional<Style>> cache = new HashMap<>();

    public synchronized void clear() { cache.clear(); }

    public synchronized Style resolve(SourcePack pack, Category cat) {
        return cache.computeIfAbsent(pack.id + "@" + pack.lastModified + "|" + cat.id, k -> Optional.ofNullable(compute(pack, cat))).orElse(null);
    }

    private static Style compute(SourcePack pack, Category cat) {
        List<String> base = new ArrayList<>();
        for (String f : pack.files()) if (cat.matches(f)) base.add(f);
        if (base.isEmpty()) return null;
        List<String> all = new ArrayList<>(ModelDependencies.expand(pack, base));
        String preview = null;
        for (String p : cat.previews) if (pack.has(p)) { preview = p; break; }
        if (preview == null) for (String f : all) if (f.endsWith(".png")) { preview = f; break; }
        if (preview == null) return null; // nur JSON ohne Bild -> nichts Vorzeigbares
        return new Style(pack, cat, all, preview);
    }
}
