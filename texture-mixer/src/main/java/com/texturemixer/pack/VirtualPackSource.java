package com.texturemixer.pack;

import com.texturemixer.MixManager;
import com.texturemixer.core.ActiveMix;
import com.texturemixer.core.Log;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSelectionConfig;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import java.util.Optional;
import java.util.function.Consumer;

/** Meldet das kombinierte Pack (ganz oben, nicht deaktivierbar) beim PackRepository an. */
public final class VirtualPackSource implements RepositorySource {
    public static final VirtualPackSource INSTANCE = new VirtualPackSource();
    public static final String PACK_ID = "texturemixer_combined";
    private boolean logged;

    @Override
    public void loadPacks(Consumer<Pack> onLoad) {
        if (!logged) { Log.info("VirtualPackSource registriert"); logged = true; }
        ActiveMix mix = MixManager.currentMix();
        if (mix.isEmpty()) return;      // nichts ausgewählt -> kein Pack, kein Eintrag in der Liste
        PackLocationInfo info = new PackLocationInfo(PACK_ID, Component.translatable("texturemixer.pack.title"),
                PackSource.BUILT_IN, Optional.empty());
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override public PackResources openPrimary(PackLocationInfo loc) { return new VirtualPackResources(loc, mix); }
            @Override public PackResources openFull(PackLocationInfo loc, Pack.Metadata meta) { return new VirtualPackResources(loc, mix); }
        };
        Pack pack = Pack.readMetaAndCreate(info, supplier, PackType.CLIENT_RESOURCES,
                new PackSelectionConfig(true, Pack.Position.TOP, false));
        if (pack != null) onLoad.accept(pack);
        else Log.warn("Kombiniertes Pack konnte nicht erstellt werden (pack.mcmeta ungültig?)");
    }
}
