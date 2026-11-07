package com.texturemixer.core;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Liest alle installierten Resource Packs aus dem resourcepacks-Ordner. */
public final class PackLibrary {
    private static final System.Logger LOG = System.getLogger("TextureMixer");

    private final Path dir;
    private volatile List<SourcePack> packs;

    public PackLibrary(Path resourcePackDir) { this.dir = resourcePackDir; }

    public synchronized List<SourcePack> packs() {
        if (packs == null) scan();
        return packs;
    }

    public synchronized void rescan() { scan(); }

    public Optional<SourcePack> find(String id) {
        for (SourcePack p : packs()) if (p.id.equals(id)) return Optional.of(p);
        return Optional.empty();
    }

    private void scan() {
        List<SourcePack> out = new ArrayList<>();
        if (Files.isDirectory(dir)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                for (Path p : ds) {
                    try {
                        SourcePack sp = null;
                        if (Files.isDirectory(p)) sp = DirSourcePack.open(p);
                        else if (p.getFileName().toString().toLowerCase().endsWith(".zip")) sp = ZipSourcePack.open(p);
                        if (sp != null) out.add(sp);
                    } catch (IOException | RuntimeException e) {
                        LOG.log(System.Logger.Level.WARNING, "Pack übersprungen: " + p.getFileName() + " (" + e.getMessage() + ")");
                    }
                }
            } catch (IOException e) {
                LOG.log(System.Logger.Level.WARNING, "resourcepacks-Ordner nicht lesbar", e);
            }
        }
        out.sort(Comparator.comparing(p -> p.displayName.toLowerCase()));
        packs = out;
    }
}
