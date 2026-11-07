package com.texturemixer.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Das fertig zusammengesetzte virtuelle Pack: Pfad -> Quellpack.
 * Nach dem Bau unveränderlich; Dateien werden erst beim Lesen aus dem Quellpack geholt (Zip-Handles werden gecached).
 */
public final class ActiveMix {
    public static final ActiveMix EMPTY = new ActiveMix(Map.of());

    private final Map<String, SourcePack> owners;
    private final Set<String> namespaces = new HashSet<>();
    private final int signature;
    private final Map<SourcePack, SourcePack.PackReader> readers = new HashMap<>();

    public ActiveMix(Map<String, SourcePack> owners) {
        this.owners = owners;
        TreeMap<String, String> sig = new TreeMap<>();
        for (var e : owners.entrySet()) {
            sig.put(e.getKey(), e.getValue().id + "@" + e.getValue().lastModified);
            String p = e.getKey();                                   // "assets/<ns>/..."
            namespaces.add(p.substring(7, p.indexOf('/', 7)));
        }
        this.signature = sig.hashCode();
    }

    public boolean isEmpty() { return owners.isEmpty(); }
    public int signature() { return signature; }
    public Set<String> namespaces() { return namespaces; }
    public Set<String> paths() { return owners.keySet(); }
    public boolean has(String path) { return owners.containsKey(path); }

    public synchronized byte[] read(String path) throws IOException {
        SourcePack p = owners.get(path);
        if (p == null) throw new IOException("Nicht im Mix: " + path);
        SourcePack.PackReader r = readers.get(p);
        if (r == null) { r = p.openReader(); readers.put(p, r); }
        byte[] data = r.read(path);
        if (data == null) throw new IOException("Datei fehlt im Pack " + p.id + ": " + path);
        return data;
    }

    /** Schließt geöffnete Zip-Handles (werden bei Bedarf neu geöffnet). */
    public synchronized void close() {
        for (SourcePack.PackReader r : readers.values()) { try { r.close(); } catch (IOException ignored) { } }
        readers.clear();
    }
}
