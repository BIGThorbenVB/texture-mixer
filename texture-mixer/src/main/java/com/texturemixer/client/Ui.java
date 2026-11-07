package com.texturemixer.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Kleine Zeichen-Helfer im flachen, dunklen Stil. */
final class Ui {
    private Ui() {}

    static void rect(GuiGraphics g, int x, int y, int w, int h, int c) { g.fill(x, y, x + w, y + h, c); }

    static void border(GuiGraphics g, int x, int y, int w, int h, int t, int c) {
        g.fill(x, y, x + w, y + t, c);
        g.fill(x, y + h - t, x + w, y + h, c);
        g.fill(x, y, x + t, y + h, c);
        g.fill(x + w - t, y, x + w, y + h, c);
    }

    static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    /** Flacher Button. */
    static void button(GuiGraphics g, Font f, int x, int y, int w, int h, String label, boolean hover, boolean enabled, boolean primary) {
        int base = primary ? Theme.alpha(Theme.accent, 0.35f) : Theme.tile;
        int bg = !enabled ? Theme.panel : hover ? Theme.lerp(base, Theme.alpha(Theme.accent, 0.6f), 0.4f) : base;
        rect(g, x, y, w, h, bg);
        border(g, x, y, w, h, 1, hover && enabled ? Theme.accent : Theme.border);
        g.drawString(f, label, x + (w - f.width(label)) / 2, y + (h - 8) / 2, enabled ? Theme.text : Theme.dim, false);
    }

    /** Tooltip mit automatischem Zeilenumbruch. */
    static void tooltip(GuiGraphics g, Font f, String text, int mx, int my, int sw, int sh) {
        List<FormattedCharSequence> lines = f.split(Component.literal(text), 190);
        int w = 0;
        for (FormattedCharSequence l : lines) w = Math.max(w, f.width(l));
        int h = lines.size() * 10 + 8;
        int x = mx + 10, y = my + 10;
        if (x + w + 10 > sw) x = mx - w - 16;
        if (y + h > sh) y = sh - h - 2;
        rect(g, x, y, w + 10, h, 0xF00A0A12);
        border(g, x, y, w + 10, h, 1, Theme.accent);
        int ty = y + 4;
        for (FormattedCharSequence l : lines) { g.drawString(f, l, x + 5, ty, 0xFFF0F0F8, false); ty += 10; }
    }

    static String ellipsize(Font f, String s, int maxW) {
        if (f.width(s) <= maxW) return s;
        return f.plainSubstrByWidth(s, maxW - f.width("…")) + "…";
    }

    static float approach(float cur, float target, float rate) {
        float d = target - cur;
        return Math.abs(d) < 0.001f ? target : cur + d * Math.min(1f, rate);
    }
}
