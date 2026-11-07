package com.texturemixer.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Baut aus der Auswahl eines Profils das virtuelle Pack. Jede Kategorie wird unabhängig aufgelöst. */
public final class MixBuilder {
    private MixBuilder() {}

    public static ActiveMix build(Map<String, String> selections, List<Category> cats, PackLibrary lib, StyleResolver styles) {
        Map<String, SourcePack> owners = new LinkedHashMap<>();
        for (Category c : cats) {
            String packId = selections.get(c.id);
            if (packId == null || packId.isBlank()) continue;
            SourcePack pack = lib.find(packId).orElse(null);
            if (pack == null) continue;                       // Pack wurde gelöscht -> Auswahl ignorieren
            Style s = styles.resolve(pack, c);
            if (s == null) continue;
            for (String f : s.files()) owners.put(f, pack);   // bei Überschneidung gewinnt die spätere Kategorie
        }
        return owners.isEmpty() ? ActiveMix.EMPTY : new ActiveMix(owners);
    }
}
