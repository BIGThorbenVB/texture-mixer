package com.texturemixer.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class ZipSourcePack extends SourcePack {
    private final Path path;
    private final String prefix;

    private ZipSourcePack(Path path, String prefix, Set<String> files) {
        super(path.getFileName().toString(), path.toFile().lastModified(), files);
        this.path = path;
        this.prefix = prefix;
    }

    static ZipSourcePack open(Path p) throws IOException {
        try (ZipFile z = new ZipFile(p.toFile())) {
            List<String> names = new ArrayList<>();
            var en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (!e.isDirectory()) names.add(e.getName().replace('\\', '/'));
            }
            String prefix = detectPrefix(names);
            if (prefix == null) return null;
            Set<String> files = new HashSet<>();
            for (String n : names) {
                if (!n.startsWith(prefix)) continue;
                String rel = n.substring(prefix.length());
                if (VALID.matcher(rel).matches()) files.add(rel);
            }
            return files.isEmpty() ? null : new ZipSourcePack(p, prefix, files);
        }
    }

    private static String detectPrefix(List<String> names) {
        for (String n : names) if (n.equals("pack.mcmeta") || n.startsWith("assets/")) return "";
        for (String n : names) {
            int i = n.indexOf('/');
            if (i > 0) {
                String rest = n.substring(i + 1);
                if (rest.equals("pack.mcmeta") || rest.startsWith("assets/")) return n.substring(0, i + 1);
            }
        }
        return null;
    }

    @Override
    public PackReader openReader() throws IOException {
        ZipFile z = new ZipFile(path.toFile());
        return new PackReader() {
            @Override public byte[] read(String rel) throws IOException {
                ZipEntry e = z.getEntry(prefix + rel);
                if (e == null) return null;
                try (InputStream in = z.getInputStream(e)) { return in.readAllBytes(); }
            }
            @Override public void close() throws IOException { z.close(); }
        };
    }
}
