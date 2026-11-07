package com.texturemixer.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.texturemixer.core.Log;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Lädt Vorschaubilder lazy als GPU-Texturen (max. N neue pro Frame) und gibt sie beim Schließen wieder frei. */
final class PreviewCache {
    record Preview(Identifier id, int w, int h) {}

    private static final Preview FAILED = new Preview(null, 0, 0);
    private final Map<String, Preview> map = new HashMap<>();
    private int counter;
    private int budget;

    void newFrame() { budget = 6; }

    /** Liefert null, solange die Vorschau (noch) nicht verfügbar ist. */
    Preview get(String key, boolean cropAnimation, Supplier<byte[]> bytes) {
        Preview p = map.get(key);
        if (p != null) return p == FAILED ? null : p;
        if (budget <= 0) return null;
        budget--;
        try {
            byte[] data = bytes.get();
            if (data == null) { map.put(key, FAILED); return null; }
            NativeImage src = NativeImage.read(new ByteArrayInputStream(data));
            int w = src.getWidth(), h = src.getHeight();
            int ch = (cropAnimation && h > w && h % w == 0) ? w : h;       // Animation: erstes Frame
            int m = Math.max(w, ch);
            int f = m >= 64 ? 1 : Math.min(8, Math.max(1, 64 / m));          // kleine Texturen pixelscharf hochskalieren
            NativeImage out = new NativeImage(w * f, ch * f, false);
            for (int y = 0; y < ch * f; y++) for (int x = 0; x < w * f; x++) out.setPixel(x, y, src.getPixel(x / f, y / f));
            src.close();
            Identifier id = McCompat.id("texturemixer", "preview/" + (counter++));
            McCompat.registerTexture(id, out);
            p = new Preview(id, w * f, ch * f);
            map.put(key, p);
            return p;
        } catch (Exception e) {
            Log.warn("Vorschau nicht ladbar: " + key, e);
            map.put(key, FAILED);
            return null;
        }
    }

    void close() {
        for (Preview p : map.values()) if (p != FAILED) McCompat.releaseTexture(p.id());
        map.clear();
    }
}
