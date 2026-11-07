package com.texturemixer.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/** Entpacktes Resource Pack (Ordner). */
final class DirSourcePack extends SourcePack {
    private final Path root;

    private DirSourcePack(Path dir, Path root, Set<String> files) {
        super(dir.getFileName().toString(), dir.toFile().lastModified(), files);
        this.root = root;
    }

    static DirSourcePack open(Path dir) throws IOException {
        Path root = dir;
        if (!Files.isDirectory(root.resolve("assets")) && !Files.exists(root.resolve("pack.mcmeta"))) {
            // Evtl. liegt das eigentliche Pack in einem Unterordner
            try (Stream<Path> s = Files.list(dir)) {
                Path inner = s.filter(Files::isDirectory)
                        .filter(d -> Files.isDirectory(d.resolve("assets")) || Files.exists(d.resolve("pack.mcmeta")))
                        .findFirst().orElse(null);
                if (inner == null) return null;
                root = inner;
            }
        }
        Set<String> files = new HashSet<>();
        Path base = root;
        try (Stream<Path> s = Files.walk(base)) {
            s.filter(Files::isRegularFile).forEach(f -> {
                String rel = base.relativize(f).toString().replace('\\', '/');
                if (VALID.matcher(rel).matches()) files.add(rel);
            });
        }
        return files.isEmpty() ? null : new DirSourcePack(dir, base, files);
    }

    @Override
    public PackReader openReader() {
        return new PackReader() {
            @Override public byte[] read(String rel) throws IOException {
                Path p = root.resolve(rel);
                return Files.isRegularFile(p) ? Files.readAllBytes(p) : null;
            }
            @Override public void close() { }
        };
    }
}
