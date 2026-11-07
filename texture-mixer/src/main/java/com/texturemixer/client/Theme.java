package com.texturemixer.client;

import com.texturemixer.MixManager;
import com.texturemixer.core.Feature;
import com.texturemixer.core.FeatureRegistry;
import com.texturemixer.core.MixerConfig;

/** Farben der Menüs, abhängig von Theme, Akzentfarbe und Transparenz aus den Einstellungen. */
final class Theme {
    static int bg, panel, tile, tileHover, border, accent, text = 0xFFE8E8F0, sub = 0xFF8A8A9C, dim = 0xFF55556A;
    static float scale = 1f;
    static boolean animations = true;
    private static int bgRgb;
    private static int bgAlpha;

    private static final int[][] PALETTE = { // panel, tile, tileHover, border, accent, background
            {0xFF0F0F16, 0xFF171720, 0xFF262634, 0xFF2C2C3C, 0xFFE6E6F0, 0x0A0A10},
            {0xFF050505, 0xFF0C0C0C, 0xFF1A1A1A, 0xFF1E1E1E, 0xFFFFFFFF, 0x000000},
            {0xFF130D20, 0xFF1B1230, 0xFF2A1D48, 0xFF33264F, 0xFFA070FF, 0x0C0716},
            {0xFF0B1220, 0xFF111B30, 0xFF1B2B4A, 0xFF22365A, 0xFF4FA3FF, 0x060C18},
            {0xFF1E0D0D, 0xFF2A1313, 0xFF442020, 0xFF512828, 0xFFFF5050, 0x140707}};

    static void refresh() {
        MixerConfig c = MixManager.config();
        Feature th = FeatureRegistry.get("theme");
        int[] p = PALETTE[Math.floorMod(th.index(c, "name"), PALETTE.length)];
        panel = p[0]; tile = p[1]; tileHover = p[2]; border = p[3]; accent = p[4]; bgRgb = p[5];
        Feature ac = FeatureRegistry.get("accent_color");
        if (ac.enabled(c)) accent = 0xFF000000 | ((int) ac.num(c, "r") << 16) | ((int) ac.num(c, "g") << 8) | (int) ac.num(c, "b");
        double transp = FeatureRegistry.get("menu_transparency").num(c, "value") / 100.0;
        bgAlpha = (int) Math.round(255 * (1.0 - transp));
        scale = (float) (FeatureRegistry.get("gui_scale").num(c, "scale") / 100.0);
        animations = FeatureRegistry.get("menu_animation").enabled(c);
    }

    /** Hintergrundfarbe mit Einblend-Fortschritt p (0..1). */
    static int background(float p) {
        int a = (int) (bgAlpha * (animations ? Math.min(1f, p) : 1f));
        return (a << 24) | bgRgb;
    }

    static int sc(int v) { return Math.round(v * scale); }
    static int alpha(int color, float a) { return (((int) (255 * a)) << 24) | (color & 0xFFFFFF); }
    static int lerp(int c1, int c2, float t) {
        int a = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
