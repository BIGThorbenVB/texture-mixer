package com.texturemixer.core;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

/** Ein installiertes Resource Pack (Zip oder Ordner) im resourcepacks-Ordner. */
public abstract class SourcePack {
    static final Pattern VALID = Pattern.compile("assets/[a-z0-9_.-]+/[a-z0-9/._-]+");

    public final String id;
    public final String displayName;
    public final long lastModified;
    protected final Set<String> files;

    protected SourcePack(String id, long lastModified, Set<String> files) {
        this.id = id;
        this.lastModified = lastModified;
        this.files = Collections.unmodifiableSet(files);
        String n = id.toLowerCase().endsWith(".zip") ? id.substring(0, id.length() - 4) : id;
        this.displayName = n.replace('_', ' ').trim();
    }

    public Set<String> files() { return files; }
    public boolean has(String path) { return files.contains(path); }

    /** Öffnet einen Reader, der mehrere Dateien effizient lesen kann. */
    public abstract PackReader openReader() throws IOException;

    public byte[] read(String path) throws IOException {
        try (PackReader r = openReader()) { return r.read(path); }
    }

    public interface PackReader extends Closeable {
        byte[] read(String path) throws IOException;
    }
}
