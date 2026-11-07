package com.texturemixer.core;

import java.util.List;

/** Ein auswählbarer Style: alle Dateien eines Packs für genau eine Kategorie (inkl. Modell-Abhängigkeiten). */
public record Style(SourcePack pack, Category category, List<String> files, String preview) { }
