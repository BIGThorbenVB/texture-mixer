package com.texturemixer.client;

import com.texturemixer.MixManager;
import com.texturemixer.core.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/** Settings-Menü: links Feature-Liste (Tabs Settings/Sounds), rechts Optionen, Beschreibung und Reset. */
public final class SettingsScreen extends Screen {
    private static final String[] GROUPS = {"PVP", "HAND", "SCREEN", "TEXTURE", "GUI", "SOUNDS"};

    private record Row(String header, int count, Feature feature) {}

    private final Screen parent;
    private String tab = "settings";
    private Feature selected;
    private final List<Row> rows = new ArrayList<>();
    private float listScroll, listTarget, rightScroll, openAnim;
    private String dragging;            // "featureId.controlKey"
    private long savedAt;
    private String lastHover = "";
    private long lastNanos = System.nanoTime();

    private int M, leftW, rowH, listY, listH, rightX, rightW, footY;
    // Hit-Rechtecke der aktuellen Frame (rechte Seite)
    private final List<Hit> hits = new ArrayList<>();
    private record Hit(int x, int y, int w, int h, String id, Runnable click) {}

    public SettingsScreen(Screen parent) {
        super(Component.translatable("texturemixer.settings"));
        this.parent = parent;
    }

    private boolean de() { return minecraft.getLanguageManager().getSelected().startsWith("de"); }
    private MixerConfig cfg() { return MixManager.config(); }

    @Override
    protected void init() {
        Theme.refresh();
        M = 14;
        leftW = Math.min(Theme.sc(250), width / 2 - 20);
        rowH = Theme.sc(22);
        listY = Theme.sc(30) + 26;
        footY = height - 30;
        listH = footY - 6 - listY;
        rightX = M + leftW + 14;
        rightW = width - rightX - M;
        openAnim = 0;
        buildRows();
        if (selected == null || !selected.tab.equals(tab)) selected = rows.stream().filter(r -> r.feature != null).map(Row::feature).findFirst().orElse(null);
    }

    private void buildRows() {
        rows.clear();
        List<Feature> fs = FeatureRegistry.tab(tab);
        for (String g : GROUPS) {
            List<Feature> in = new ArrayList<>();
            for (Feature f : fs) if (f.group.equals(g)) in.add(f);
            if (in.isEmpty()) continue;
            rows.add(new Row(g, in.size(), null));
            for (Feature f : in) rows.add(new Row(null, 0, f));
        }
    }

    private void changed(boolean persist) {
        Features.onSettingsChanged();
        if (persist) { MixManager.saveConfig(); savedAt = System.currentTimeMillis(); }
    }

    private void save() { MixManager.saveConfig(); savedAt = System.currentTimeMillis(); }

    @Override public void onClose() {
        save();
        MixManager.applyIfChanged();          // z. B. nach "Alle Styles zurücksetzen" oder Style-Wahl
        minecraft.setScreen(parent);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) { }

    // ------------------------------------------------------------------ Anzeige-Texte
    private String valueText(Feature f) {
        if (!f.available) return Component.translatable("texturemixer.soon").getString();
        if (f.link != null) return MixManager.config().active().selections.containsKey(f.link)
                ? MixManager.library().find(MixManager.config().active().selections.get(f.link)).map(p -> p.displayName).orElse("?")
                : Component.translatable("texturemixer.default").getString();
        if (f.controls.isEmpty()) return "";
        Feature.Control c = f.controls.get(0);
        if (f.toggle) return "";
        return controlText(f, c);
    }

    private String controlText(Feature f, Feature.Control c) {
        double v = f.num(cfg(), c.key);
        if (c.type == Feature.Type.CYCLE) return c.option((int) Math.round(v), de());
        String n = c.step < 1 ? String.format(Locale.ROOT, "%.1f", v) : String.valueOf((int) Math.round(v));
        return n + c.unit;
    }

    // ------------------------------------------------------------------ Rendering
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        float rate = Theme.animations ? dt * 14f : 1f;
        openAnim = Math.min(1f, openAnim + (Theme.animations ? dt * 6f : 1f));
        listScroll = Ui.approach(listScroll, listTarget, rate);
        hits.clear();
        String tip = null;

        g.fill(0, 0, width, height, Theme.background(openAnim));

        // Kopfzeile
        boolean backHov = Ui.in(mx, my, M, 6, 56, 16);
        Ui.button(g, font, M, 6, 56, 16, "‹ " + Component.translatable("texturemixer.back").getString(), backHov, true, false);
        g.drawString(font, "TEXTURE MIXER", M + 68, 10, Theme.text, false);
        g.drawString(font, "|  " + Component.translatable("texturemixer.settings").getString().toUpperCase(Locale.ROOT), M + 68 + font.width("TEXTURE MIXER") + 6, 10, Theme.sub, false);
        if (System.currentTimeMillis() - savedAt < 4000)
            g.drawString(font, "✔ " + Component.translatable("texturemixer.saved").getString(), width - M - 60, 10, Theme.sub, false);
        Ui.rect(g, 0, 28, width, 1, Theme.border);

        // Tabs
        int tw = leftW / 2;
        int ty = 34;
        int nSet = FeatureRegistry.tab("settings").size(), nSnd = FeatureRegistry.tab("sounds").size();
        tab(g, mx, my, M, ty, tw - 2, Component.translatable("texturemixer.tab.settings").getString() + " " + nSet, tab.equals("settings"));
        tab(g, mx, my, M + tw, ty, tw, Component.translatable("texturemixer.tab.sounds").getString() + " " + nSnd, tab.equals("sounds"));

        // Liste links
        int total = 0;
        for (Row r : rows) total += r.header != null ? 16 : rowH;
        float maxScroll = Math.max(0, total - listH);
        listTarget = Math.max(0, Math.min(maxScroll, listTarget));
        listScroll = Math.max(0, Math.min(maxScroll, listScroll));
        g.enableScissor(M, listY, M + leftW, listY + listH);
        int y = Math.round(listY - listScroll);
        String hoverId = "";
        for (Row r : rows) {
            if (r.header != null) {
                if (y + 16 > listY && y < listY + listH) {
                    Ui.rect(g, M + 2, y + 5, 2, 8, Theme.accent);
                    g.drawString(font, r.header + "  " + r.count, M + 8, y + 5, Theme.dim, false);
                }
                y += 16;
                continue;
            }
            Feature f = r.feature;
            if (y + rowH > listY && y < listY + listH) {
                boolean hov = Ui.in(mx, my, M, y, leftW, rowH) && my >= listY && my < listY + listH;
                boolean sel = f == selected;
                Ui.rect(g, M, y, leftW, rowH - 1, sel ? Theme.tileHover : hov ? Theme.tile : 0);
                if (sel) Ui.rect(g, M, y, 2, rowH - 1, Theme.accent);
                g.renderItem(McCompat.icon(f.icon), M + 6, y + (rowH - 16) / 2);
                String name = f.name(de());
                int nameColor = f.available ? (sel ? Theme.text : 0xFFC8C8D6) : Theme.dim;
                g.drawString(font, name, M + 28, y + (rowH - 8) / 2, nameColor, false);
                int rx = M + leftW - 8;
                if (f.toggle) { pill(g, rx - 26, y + (rowH - 10) / 2, f.enabled(cfg()), f.available); }
                else {
                    String vt = Ui.ellipsize(font, valueText(f), leftW / 3);
                    g.drawString(font, vt, rx - font.width(vt), y + (rowH - 8) / 2, Theme.dim, false);
                }
                if (hov) { hoverId = f.id; if (tip == null) tip = f.desc(de()); }
            }
            y += rowH;
        }
        g.disableScissor();
        if (!hoverId.equals(lastHover)) { if (!hoverId.isEmpty()) UiSounds.hover(); lastHover = hoverId; }
        if (maxScroll > 0) {
            int bh = Math.max(16, (int) (listH * (listH / (float) total)));
            Ui.rect(g, M + leftW + 3, listY + (int) ((listH - bh) * (listScroll / maxScroll)), 2, bh, Theme.accent);
        }

        // Rechte Seite
        Ui.rect(g, rightX - 7, 34, 1, footY - 40, Theme.border);
        String ctlTip = renderRight(g, mx, my);
        if (ctlTip != null) tip = ctlTip;

        // Fußzeile
        Ui.rect(g, 0, footY - 2, width, 1, Theme.border);
        g.drawString(font, Component.translatable("texturemixer.pick").getString(), M, height - 14, Theme.dim, false);
        Ui.button(g, font, width - M - 70, footY + 4, 70, 16, Component.translatable("texturemixer.save").getString(), Ui.in(mx, my, width - M - 70, footY + 4, 70, 16), true, true);

        if (tip != null) Ui.tooltip(g, font, tip, mx, my, width, height);
    }

    private void tab(GuiGraphics g, int mx, int my, int x, int y, int w, String label, boolean active) {
        boolean hov = Ui.in(mx, my, x, y, w, 18);
        Ui.rect(g, x, y, w, 18, active ? Theme.alpha(Theme.accent, 0.35f) : hov ? Theme.tile : Theme.panel);
        if (active) Ui.rect(g, x, y + 17, w, 1, Theme.accent);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + 5, active ? Theme.text : Theme.sub, false);
    }

    private void pill(GuiGraphics g, int x, int y, boolean on, boolean enabled) {
        int bg = !enabled ? Theme.panel : on ? Theme.accent : Theme.border;
        Ui.rect(g, x, y, 24, 10, bg);
        Ui.rect(g, on ? x + 14 : x + 2, y + 2, 8, 6, enabled ? 0xFFF0F0F8 : Theme.dim);
    }

    /** Rechte Seite; gibt ggf. einen Tooltip-Text zurück. */
    private String renderRight(GuiGraphics g, int mx, int my) {
        if (selected == null) return null;
        Feature f = selected;
        String tip = null;
        int x = rightX, y = 38;
        g.renderItem(McCompat.icon(f.icon), x, y - 2);
        g.drawString(font, f.name(de()), x + 22, y, Theme.text, false);
        g.drawString(font, f.group, x + 22, y + 10, Theme.dim, false);
        y += 26;

        // Beschreibung
        List<FormattedCharSequence> lines = font.split(Component.literal(f.desc(de())), rightW - 16);
        int dh = lines.size() * 10 + 12;
        Ui.rect(g, x, y, rightW, dh, Theme.panel);
        Ui.border(g, x, y, rightW, dh, 1, Theme.border);
        int ly = y + 6;
        for (FormattedCharSequence l : lines) { g.drawString(font, l, x + 8, ly, Theme.sub, false); ly += 10; }
        y += dh + 8;

        int rowW = Math.min(rightW, 420);
        // Haupt-Schalter
        if (f.toggle) {
            boolean on = f.enabled(cfg());
            hover(g, mx, my, x, y, rowW, 18);
            g.drawString(font, Component.translatable("texturemixer.enabled").getString(), x + 8, y + 5, f.available ? Theme.text : Theme.dim, false);
            pill(g, x + rowW - 30, y + 4, on, f.available);
            final Feature ff = f;
            if (f.available) hits.add(new Hit(x, y, rowW, 18, "toggle", () -> { cfg().settings.put(ff.enabledKey(), !ff.enabled(cfg())); UiSounds.click(); changed(true); }));
            y += 22;
        }
        // Link zum Texture Mixer
        if (f.link != null) {
            Ui.button(g, font, x, y, 130, 18, Component.translatable("texturemixer.choose_style").getString(), Ui.in(mx, my, x, y, 130, 18), true, true);
            final String link = f.link;
            hits.add(new Hit(x, y, 130, 18, "link", () -> {
                MixManager.saveConfig();
                TextureMixerScreen s = new TextureMixerScreen(this);
                MixManager.config().lastCategory = link;
                minecraft.setScreen(s);
            }));
            g.drawString(font, Component.translatable("texturemixer.active", valueText(f)).getString(), x + 138, y + 5, Theme.sub, false);
            y += 26;
        }
        // Steuerelemente
        for (Feature.Control c : f.controls) {
            if (!f.available) break;
            boolean enabled = f.enabled(cfg()) || !f.toggle && f.available;
            int cy = y;
            boolean hov = hover(g, mx, my, x, cy, rowW, 18);
            g.drawString(font, c.name(de()), x + 8, cy + 5, enabled ? Theme.text : Theme.dim, false);
            if (hov) tip = c.desc(de());
            int cx = x + rowW / 2 - 10, cw = rowW / 2 + 6;
            if (c.type == Feature.Type.SLIDER) {
                double v = f.num(cfg(), c.key);
                float t = (float) ((v - c.min) / (c.max - c.min));
                int sx = cx, sw = cw - 46;
                Ui.rect(g, sx, cy + 7, sw, 4, Theme.border);
                Ui.rect(g, sx, cy + 7, Math.round(sw * t), 4, enabled ? Theme.accent : Theme.dim);
                Ui.rect(g, sx + Math.round(sw * t) - 2, cy + 4, 4, 10, enabled ? 0xFFF0F0F8 : Theme.dim);
                String vt = controlText(f, c);
                g.drawString(font, vt, x + rowW - font.width(vt) - 4, cy + 5, Theme.sub, false);
                final String id = f.id + "." + c.key;
                final Feature ff = f; final Feature.Control cc = c; final int fsx = sx, fsw = sw;
                hits.add(new Hit(sx - 4, cy, sw + 8, 18, "slider:" + id, () -> { dragging = id; setSlider(ff, cc, fsx, fsw, mxNow); }));
            } else {
                String vt = c.option((int) Math.round(f.num(cfg(), c.key)), de());
                Ui.button(g, font, cx, cy, cw - 6, 16, vt, Ui.in(mx, my, cx, cy, cw - 6, 16) && enabled, enabled, false);
                final Feature ff = f; final Feature.Control cc = c;
                hits.add(new Hit(cx, cy, cw - 6, 16, "cycle", () -> {
                    int n = cc.options.size();
                    cfg().settings.put(ff.key(cc), (double) Math.floorMod((int) Math.round(ff.num(cfg(), cc.key)) + 1, n));
                    UiSounds.click(); changed(true);
                }));
            }
            y += 22;
        }
        // Farbvorschau
        boolean hasRgb = f.available && f.controls.stream().anyMatch(c -> c.key.equals("r")) && f.controls.stream().anyMatch(c -> c.key.equals("b"));
        if (hasRgb) {
            int col = 0xFF000000 | ((int) f.num(cfg(), "r") << 16) | ((int) f.num(cfg(), "g") << 8) | (int) f.num(cfg(), "b");
            Ui.rect(g, x + rowW - 40, 38, 36, 14, col);
            Ui.border(g, x + rowW - 40, 38, 36, 14, 1, Theme.border);
        }

        // Reset-Box (unten rechts)
        int bw = Math.min(rightW, 520);
        int bh = 64, by = Math.max(y + 10, footY - 8 - bh);
        Ui.rect(g, x, by, bw, bh, Theme.panel);
        Ui.border(g, x, by, bw, bh, 1, Theme.border);
        Ui.rect(g, x + 8, by + 7, 2, 8, Theme.accent);
        g.drawString(font, Component.translatable("texturemixer.reset.title").getString(), x + 14, by + 7, Theme.text, false);
        int b1w = (bw - 32) / 3;
        String[] labels = {Component.translatable("texturemixer.reset.feature").getString(), Component.translatable("texturemixer.reset.settings").getString(), Component.translatable("texturemixer.reset.styles").getString()};
        for (int i = 0; i < 3; i++) {
            int bx = x + 8 + i * (b1w + 8);
            boolean h = Ui.in(mx, my, bx, by + 22, b1w, 18);
            Ui.rect(g, bx, by + 22, b1w, 18, h ? 0xFF5A2626 : 0xFF3E1C1C);
            Ui.border(g, bx, by + 22, b1w, 18, 1, h ? 0xFFFF6060 : 0xFF5A2A2A);
            String l = Ui.ellipsize(font, labels[i], b1w - 6);
            g.drawString(font, l, bx + (b1w - font.width(l)) / 2, by + 27, Theme.text, false);
            final int idx = i;
            hits.add(new Hit(bx, by + 22, b1w, 18, "reset" + i, () -> doReset(idx)));
        }
        g.drawString(font, Component.translatable("texturemixer.reset.note").getString(), x + 8, by + 46, Theme.dim, false);
        return tip;
    }

    private boolean hover(GuiGraphics g, int mx, int my, int x, int y, int w, int h) {
        boolean hov = Ui.in(mx, my, x, y, w, h);
        if (hov) Ui.rect(g, x, y, w, h, Theme.tile);
        return hov;
    }

    private void doReset(int which) {
        MixerConfig c = cfg();
        switch (which) {
            case 0 -> { if (selected != null) { selected.reset(c); if (selected.link != null) c.active().selections.remove(selected.link); } }
            case 1 -> c.settings.clear();
            default -> c.active().selections.clear();
        }
        UiSounds.click();
        changed(true);
    }

    // ------------------------------------------------------------------ Eingaben
    private double mxNow;

    private void setSlider(Feature f, Feature.Control c, int sx, int sw, double mx) {
        double t = Math.max(0, Math.min(1, (mx - sx) / sw));
        double v = c.min + t * (c.max - c.min);
        v = Math.round(v / c.step) * c.step;
        v = Math.max(c.min, Math.min(c.max, v));
        v = Math.round(v * 1000.0) / 1000.0;
        cfg().settings.put(f.key(c), v);
        Features.onSettingsChanged();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean dbl) {
        double mx = event.x(), my = event.y();
        mxNow = mx;
        if (event.button() != 0) return super.mouseClicked(event, dbl);
        if (Ui.in(mx, my, M, 6, 56, 16)) { UiSounds.click(); onClose(); return true; }
        if (Ui.in(mx, my, width - M - 70, footY + 4, 70, 16)) { UiSounds.click(); onClose(); return true; }
        int tw = leftW / 2;
        if (Ui.in(mx, my, M, 34, tw - 2, 18)) { switchTab("settings"); return true; }
        if (Ui.in(mx, my, M + tw, 34, tw, 18)) { switchTab("sounds"); return true; }
        // Liste
        if (Ui.in(mx, my, M, listY, leftW, listH)) {
            int y = Math.round(listY - listScroll);
            for (Row r : rows) {
                int h = r.header != null ? 16 : rowH;
                if (r.feature != null && my >= y && my < y + h) {
                    Feature f = r.feature;
                    if (f.toggle && f.available && mx >= M + leftW - 40) { cfg().settings.put(f.enabledKey(), !f.enabled(cfg())); UiSounds.click(); changed(true); }
                    else { selected = f; UiSounds.click(); }
                    return true;
                }
                y += h;
            }
        }
        for (Hit h : hits) if (Ui.in(mx, my, h.x, h.y, h.w, h.h)) { h.click.run(); return true; }
        return super.mouseClicked(event, dbl);
    }

    private void switchTab(String t) { tab = t; listScroll = listTarget = 0; selected = null; buildRows(); selected = rows.stream().filter(r -> r.feature != null).map(Row::feature).findFirst().orElse(null); UiSounds.click(); }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null && selected != null) {
            for (Hit h : hits) if (h.id.equals("slider:" + dragging)) {
                Feature.Control c = null;
                for (Feature.Control k : selected.controls) if ((selected.id + "." + k.key).equals(dragging)) c = k;
                if (c != null) setSlider(selected, c, h.x + 4, h.w - 8, event.x());
                return true;
            }
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) { dragging = null; changed(true); return true; }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx < rightX - 8) { listTarget -= (float) (sy * rowH * 2); if (!Theme.animations) listScroll = listTarget; }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(event);
    }
}
