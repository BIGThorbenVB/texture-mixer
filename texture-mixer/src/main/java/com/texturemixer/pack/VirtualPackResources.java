package com.texturemixer.pack;

import com.texturemixer.core.ActiveMix;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** Ein virtuelles Resource Pack, das genau die ausgewählten Dateien aus den Quell-Packs ausliefert. */
public final class VirtualPackResources extends AbstractPackResources {
    private static final String MCMETA = """
            {"pack":{"description":"Texture Mixer - combined pack","pack_format":75,"min_format":[75,0],"max_format":[9999,0]}}""";

    private final ActiveMix mix;

    public VirtualPackResources(PackLocationInfo location, ActiveMix mix) {
        super(location);
        this.mix = mix;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length == 1 && elements[0].equals("pack.mcmeta"))
            return () -> new ByteArrayInputStream(MCMETA.getBytes(StandardCharsets.UTF_8));
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        if (type != PackType.CLIENT_RESOURCES) return null;
        String path = "assets/" + id.getNamespace() + "/" + id.getPath();
        if (!mix.has(path)) return null;
        return () -> new ByteArrayInputStream(mix.read(path));
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) return;
        String base = "assets/" + namespace + "/";
        for (String path : mix.paths()) {
            if (!path.startsWith(base)) continue;
            String rel = path.substring(base.length());
            if (rel.equals(prefix) || rel.startsWith(prefix + "/")) {
                output.accept(Identifier.fromNamespaceAndPath(namespace, rel), () -> new ByteArrayInputStream(mix.read(path)));
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? mix.namespaces() : Set.of();
    }

    @Override
    public void close() { /* Dateihandles gehören dem ActiveMix und werden dort geschlossen */ }
}
