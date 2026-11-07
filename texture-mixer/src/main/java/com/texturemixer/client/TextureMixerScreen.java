package com.texturemixer.client;

import com.texturemixer.MixManager;
import com.texturemixer.core.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/** Der Texture-Selector: Kategorie-Leiste, Raster mit Vorschauen, Suche, Favoriten, Profile. */
public final class TextureMixerScreen extends Screen {
    private record Entry(String packId, String name, Style style) {}
    private static final class Btn {
        final java.util.function.Supplier<String> label; final Runnable action; int x, y, w, h; boolean primary, text;
        Btn(java.util.function.Supplier<String> l, Runnable a, boolean primary) { label = l; action = a; this.primary = primary; }
    }

    private final Screen parent;
    private MixerConfig draft;
    private List<Category> cats;
    private int catIndex;
    private String search = "";
    private boolean favOnly;
    private boolean creatingProfile;
    private EditBox searchBox, nameBox;
    private final List<Entry> entries = new ArrayList<>();
    private final PreviewCache previews = new PreviewCache();
    private final Map<String, String> vanillaPreview = new HashMap<>();
    private final List<Btn> buttons = new ArrayList<>();
    private Btn favBtn, okBtn, cancelBtn;

    private float scroll, scrollTarget, openAnim;
    private float[] hoverAnim = new float[0];
    private long lastNanos = System.nanoTime();

    // Layout
    private int M, catY, catSize, catGap, catX, headerY, gridX, gridY, gridW, gridH, tileW, tileH, gap, cols, btnY;

    public TextureMixerScreen(Screen parent) {
        super(Component.translatable("texturemixer.title"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ Aufbau
    @Override
    protected void init() {
        Theme.refresh();
        draft = MixManager.config().deepCopy();
        cats = MixManager.categories();
        catIndex = 0;
        for (int i = 0; i < cats.size(); i++) if (cats.get(i).id.equals(draft.lastCategory)) catIndex = i;
        rebuildEntries();

        M = 14;
        int topW = Math.min(160, width / 4);
        searchBox = new EditBox(font, width - M - topW + 6, 10, topW - 10, 12, Component.translatable("texturemixer.search"));
        searchBox.setBordered(false);
        searchBox.setHint(Component.translatable("texturemixer.search"));
        searchBox.setValue(search);
        searchBox.setResponder(s -> { search = s; rebuildEntries(); scrollTarget = 0; });
        addRenderableWidget(searchBox);

        nameBox = new EditBox(font, width / 2 - 90, height / 2 - 4, 180, 16, Component.literal("name"));
        nameBox.setMaxLength(24);
        nameBox.visible = false;
        addRenderableWidget(nameBox);

        buttons.clear();
        favBtn = new Btn(() -> "★ " + Component.translatable("texturemixer.favorites").getString(), () -> { favOnly = !favOnly; rebuildEntries(); scrollTarget = 0; }, false);
        buttons.add(favBtn);
        Btn prev = new Btn(() -> "◀", () -> cycleProfile(-1), false);
        Btn next = new Btn(() -> "▶", () -> cycleProfile(1), false);
        Btn profile = new Btn(() -> Component.translatable("texturemixer.profile", draft.activeProfile).getString(), () -> {}, false);
        profile.text = true;
        Btn newP = new Btn(() -> Component.translatable("texturemixer.profile.new").getString(), this::startNewProfile, false);
        Btn delP = new Btn(() -> Component.translatable("texturemixer.profile.delete").getString(), this::deleteProfile, false);
        Btn resCat = new Btn(() -> Component.translatable("texturemixer.reset_category").getString(), this::resetCategory, false);
        Btn resAll = new Btn(() -> Component.translatable("texturemixer.reset_all").getString(), this::resetAll, false);
        Btn rescan = new Btn(() -> Component.translatable("texturemixer.rescan").getString(), () -> { MixManager.rescan(); previews.close(); vanillaPreview.clear(); rebuildEntries(); }, false);
        Btn settings = new Btn(() -> "⚙ " + Component.translatable("texturemixer.settings").getString(), this::openSettings, false);
        Btn done = new Btn(() -> Component.translatable("texturemixer.done").getString(), this::done, true);
        okBtn = new Btn(() -> Component.translatable("texturemixer.ok").getString(), this::confirmProfile, true);
        cancelBtn = new Btn(() -> "✕", () -> creatingProfile = false, false);
        buttons.addAll(List.of(prev, profile, next, newP, delP, resCat, resAll, rescan, settings, done));

        btnY = height - Theme.sc(34);
        int x = M;
        for (Btn b : List.of(prev, profile, next, newP, delP)) { b.w = (b == profile ? Math.max(90, font.width(b.label.get()) + 14) : font.width(b.label.get()) + 14); b.x = x; b.y = btnY; b.h = 16; x += b.w + 3; }
        int rx = width - M;
        for (Btn b : List.of(done, settings, rescan, resAll, resCat)) { b.w = font.width(b.label.get()) + 16; rx -= b.w; b.x = rx; b.y = btnY; b.h = 16; rx -= 4; }
        favBtn.w = font.width(favBtn.label.get()) + 16; favBtn.h = 16; favBtn.x = width - M - topW - favBtn.w - 6; favBtn.y = 6;
        okBtn.w = 50; okBtn.h = 16; okBtn.x = width / 2 + 94; okBtn.y = height / 2 - 4;
        cancelBtn.w = 20; cancelBtn.h = 16; cancelBtn.x = width / 2 + 148; cancelBtn.y = height / 2 - 4;

        catSize = Math.max(Theme.sc(20), Math.min(Theme.sc(34), (width - 2 * M) / Math.max(1, cats.size()) - 3));
        catGap = 3;
        catX = M;
        catY = Theme.sc(30);
        headerY = catY + catSize + 10;
        gridX = M; gridY = headerY + 16; gridW = width - 2 * M - 6; gridH = btnY - 22 - gridY;
        tileW = Theme.sc(84); tileH = Theme.sc(92); gap = Theme.sc(8);
        cols = Math.max(1, (gridW + gap) / (tileW + gap));
        gridX = M + (gridW - (cols * (tileW + gap) - gap)) / 2;
        openAnim = 0;
    }

    private Category cat() { return cats.get(catIndex); }

    private boolean de() { return minecraft != null && minecraft.getLanguageManager().getSelected().startsWith("de"); }

    private String catName(Category c) { return Component.translatableWithFallback("texturemixer.category." + c.id, c.name).getString(); }

    private void rebuildEntries() {
        entries.clear();
        if (cats == null || cats.isEmpty()) return;
        Category c = cat();
        String q = search.toLowerCase(Locale.ROOT).trim();
        List<Entry> list = new ArrayList<>();
        for (SourcePack p : MixManager.library().packs()) {
            Style s = MixManager.styles().resolve(p, c);
            if (s == null) continue;
            if (!q.isEmpty() && !p.displayName.toLowerCase(Locale.ROOT).contains(q)) continue;
            if (favOnly && !draft.favorites.contains(c.id + "|" + p.id)) continue;
            list.add(new Entry(p.id, p.displayName, s));
        }
        list.sort(Comparator.<Entry>comparingInt(e -> draft.favorites.contains(c.id + "|" + e.packId) ? 0 : 1)
                .thenComparing(e -> e.name.toLowerCase(Locale.ROOT)));
        String def = Component.translatable("texturemixer.default").getString();
        if (!favOnly && (q.isEmpty() || def.toLowerCase(Locale.ROOT).contains(q))) entries.add(new Entry(null, def, null));
        entries.addAll(list);
        hoverAnim = new float[entries.size()];
    }

    // ------------------------------------------------------------------ Aktionen
    private String selected() { return draft.active().selections.get(cat().id); }

    private void select(Entry e) {
        if (e.packId == null) draft.active().selections.remove(cat().id);
        else draft.active().selections.put(cat().id, e.packId);
        UiSounds.click();
    }

    private void toggleFavorite(Entry e) {
        if (e.packId == null) return;
        String k = cat().id + "|" + e.packId;
        if (!draft.favorites.remove(k)) draft.favorites.add(k);
        UiSounds.click();
        if (favOnly) rebuildEntries();
    }

    private void setCategory(int i) {
        i = Math.floorMod(i, cats.size());
        if (i == catIndex) return;
        catIndex = i;
        draft.lastCategory = cat().id;
        scroll = scrollTarget = 0;
        rebuildEntries();
        UiSounds.click();
    }

    private void resetCategory() { draft.active().selections.remove(cat().id); UiSounds.click(); }
    private void resetAll() { draft.active().selections.clear(); UiSounds.click(); }

    private void cycleProfile(int d) {
        List<String> names = new ArrayList<>(draft.profiles.keySet());
        draft.activeProfile = names.get(Math.floorMod(names.indexOf(draft.activeProfile) + d, names.size()));
        UiSounds.click();
    }

    private void startNewProfile() { creatingProfile = true; nameBox.setValue(""); nameBox.visible = true; setFocused(nameBox); nameBox.setFocused(true); }

    private void confirmProfile() {
        String n = nameBox.getValue().trim();
        if (!n.isEmpty() && !draft.profiles.containsKey(n)) {
            MixerConfig.Profile p = new MixerConfig.Profile();
            p.selections = new LinkedHashMap<>(draft.active().selections);      // Kopie des aktuellen Profils
            draft.profiles.put(n, p);
            draft.activeProfile = n;
        }
        creatingProfile = false; nameBox.visible = false;
    }

    private void deleteProfile() {
        if (draft.profiles.size() <= 1) return;
        draft.profiles.remove(draft.activeProfile);
        draft.activeProfile = draft.profiles.keySet().iterator().next();
        UiSounds.click();
    }

    private void openSettings() {
        MixManager.commit(draft);                         // Auswahl sichern (noch ohne Reload)
        minecraft.setScreen(new SettingsScreen(this));
    }

    private void done() {
        MixManager.commit(draft);
        MixManager.applyIfChanged();                      // baut das Pack neu und lädt Resource Packs neu
        UiSounds.click();
        minecraft.setScreen(parent);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }   // Esc = Abbrechen (Arbeitskopie wird verworfen)
    @Override public void removed() { previews.close(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) { /* eigener Hintergrund in render() */ }

    // ------------------------------------------------------------------ Rendering
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        float rate = Theme.animations ? dt * 14f : 1f;
        openAnim = Math.min(1f, openAnim + (Theme.animations ? dt * 6f : 1f));
        scroll = Ui.approach(scroll, scrollTarget, rate);
        previews.newFrame();

        g.fill(0, 0, width, height, Theme.background(openAnim));
        boolean modal = creatingProfile;
        int hx = modal ? -1000 : mx, hy = modal ? -1000 : my;
        String tooltip = null;

        // Kopfzeile
        g.drawString(font, "TEXTURE MIXER", M, 12, Theme.text, false);
        g.drawString(font, "|  " + Component.translatable("texturemixer.subtitle").getString(), M + font.width("TEXTURE MIXER") + 6, 12, Theme.sub, false);
        Ui.rect(g, 0, 26, width, 1, Theme.border);
        int topW = Math.min(160, width / 4);
        int sbx = width - M - topW;
        Ui.rect(g, sbx, 6, topW, 16, Theme.panel);
        Ui.border(g, sbx, 6, topW, 16, 1, searchBox.isFocused() ? Theme.accent : Theme.border);

        // Kategorie-Leiste
        for (int i = 0; i < cats.size(); i++) {
            int x = catX + i * (catSize + catGap);
            boolean hov = Ui.in(hx, hy, x, catY, catSize, catSize), sel = i == catIndex;
            Ui.rect(g, x, catY, catSize, catSize, sel ? Theme.tileHover : hov ? Theme.tile : Theme.panel);
            if (sel) Ui.border(g, x, catY, catSize, catSize, 1, Theme.accent);
            g.renderItem(McCompat.icon(cats.get(i).icon), x + (catSize - 16) / 2, catY + (catSize - 16) / 2);
            if (hov) tooltip = catName(cats.get(i));
        }

        // Titel des Rasters
        Category c = cat();
        Ui.rect(g, M, headerY, 2, 9, Theme.accent);
        g.drawString(font, catName(c).toUpperCase(Locale.ROOT), M + 6, headerY, Theme.text, false);
        g.drawString(font, Component.translatable("texturemixer.styles", entries.size()).getString(), M + 12 + font.width(catName(c).toUpperCase(Locale.ROOT)), headerY, Theme.dim, false);
        String sel = selected();
        String activeName = sel == null ? Component.translatable("texturemixer.default").getString()
                : MixManager.library().find(sel).map(p -> p.displayName).orElse(sel + " ?");
        String act = Component.translatable("texturemixer.active", activeName).getString();
        g.drawString(font, Ui.ellipsize(font, act, width / 2), width - M - font.width(Ui.ellipsize(font, act, width / 2)), headerY, Theme.sub, false);

        // Raster
        int rows = (entries.size() + cols - 1) / cols;
        float content = rows * (tileH + gap);
        float maxScroll = Math.max(0, content - gridH - 0);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        g.enableScissor(0, gridY, width, gridY + gridH);
        if (entries.isEmpty()) g.drawCenteredString(font, Component.translatable("texturemixer.empty").getString(), width / 2, gridY + 30, Theme.dim);
        for (int i = 0; i < entries.size(); i++) {
            int col = i % cols, row = i / cols;
            int x = gridX + col * (tileW + gap);
            int y = Math.round(gridY + row * (tileH + gap) - scroll);
            if (y + tileH < gridY || y > gridY + gridH) continue;
            Entry e = entries.get(i);
            boolean hov = Ui.in(hx, hy, x, y, tileW, tileH) && hy >= gridY && hy < gridY + gridH;
            hoverAnim[i] = Ui.approach(hoverAnim[i], hov ? 1f : 0f, rate * 1.4f);
            boolean isSel = Objects.equals(e.packId, sel);
            renderTile(g, e, x, y, hoverAnim[i], isSel, c);
            if (hov && font.width(e.name) > tileW - 6) tooltip = e.name;
        }
        g.disableScissor();
        if (maxScroll > 0) {                                  // Scrollbalken
            int bx = width - M + 1, bh = Math.max(20, (int) (gridH * (gridH / content)));
            int by = gridY + (int) ((gridH - bh) * (scroll / maxScroll));
            Ui.rect(g, bx, gridY, 3, gridH, Theme.panel);
            Ui.rect(g, bx, by, 3, bh, Theme.accent);
        }

        // Fußzeile
        Ui.rect(g, 0, btnY - 6, width, 1, Theme.border);
        for (Btn b : buttons) {
            boolean fav = b == favBtn;
            if (b.text) { Ui.rect(g, b.x, b.y, b.w, b.h, Theme.panel); g.drawString(font, Ui.ellipsize(font, b.label.get(), b.w - 8), b.x + 4, b.y + 4, Theme.text, false); continue; }
            Ui.button(g, font, b.x, b.y, b.w, b.h, b.label.get(), Ui.in(hx, hy, b.x, b.y, b.w, b.h), !(b.label.get().equals(Component.translatable("texturemixer.profile.delete").getString()) && draft.profiles.size() <= 1), b.primary || (fav && favOnly));
        }
        g.drawString(font, Component.translatable("texturemixer.hint").getString(), M, height - 12, Theme.dim, false);

        super.render(g, hx, hy, pt);                            // Suchfeld

        if (modal) renderNameDialog(g, mx, my);
        else if (tooltip != null) Ui.tooltip(g, font, tooltip, mx, my, width, height);
    }

    private void renderTile(GuiGraphics g, Entry e, int x, int y, float hov, boolean sel, Category c) {
        Ui.rect(g, x, y, tileW, tileH, Theme.lerp(Theme.tile, Theme.tileHover, hov));
        Ui.border(g, x, y, tileW, tileH, sel ? 2 : 1, sel ? Theme.accent : Theme.lerp(Theme.border, Theme.accent, hov * 0.6f));

        int box = tileW - 16;
        int px = x + 8, py = y + 8;
        PreviewCache.Preview pv;
        if (e.style == null) {
            String path = vanillaPreview.computeIfAbsent(c.id, k -> {
                for (String p : c.previews) if (McCompat.readVanilla(p) != null) return p;
                return "";
            });
            pv = path.isEmpty() ? null : previews.get("vanilla|" + path, cropFor(path), () -> McCompat.readVanilla(path));
        } else {
            String path = e.style.preview();
            pv = previews.get(e.packId + "|" + path, cropFor(path), () -> {
                try { return e.style.pack().read(path); } catch (Exception ex) { return null; }
            });
        }
        if (pv != null) {
            float s = Math.min((float) box / pv.w(), (float) box / pv.h());
            int dw = Math.max(1, (int) (pv.w() * s)), dh = Math.max(1, (int) (pv.h() * s));
            McCompat.blit(g, pv.id(), px + (box - dw) / 2, py + (box - dh) / 2, dw, dh, pv.w(), pv.h());
        } else {
            g.drawCenteredString(font, "…", x + tileW / 2, py + box / 2 - 4, Theme.dim);
        }
        String name = Ui.ellipsize(font, e.name, tileW - 6);
        g.drawString(font, name, x + (tileW - font.width(name)) / 2, y + tileH - 13, sel ? Theme.text : Theme.sub, false);

        if (sel) {                                              // Häkchen
            Ui.rect(g, x + 3, y + 3, 11, 11, Theme.accent);
            g.drawString(font, "✔", x + 5, y + 4, 0xFF000000, false);
        }
        if (e.packId != null) {
            boolean fav = draft.favorites.contains(c.id + "|" + e.packId);
            if (fav) g.drawString(font, "★", x + tileW - 12, y + 4, 0xFFFFD34D, false);
            else if (hov > 0.3f) g.drawString(font, "☆", x + tileW - 12, y + 4, Theme.dim, false);
        }
    }

    private static boolean cropFor(String p) { return p.contains("/textures/item/") || p.contains("/textures/block/") || p.contains("/textures/particle/"); }

    private void renderNameDialog(GuiGraphics g, int mx, int my) {
        g.fill(0, 0, width, height, 0xA0000000);
        int w = 250, h = 62, x = width / 2 - w / 2, y = height / 2 - 28;
        Ui.rect(g, x, y, w, h, Theme.panel);
        Ui.border(g, x, y, w, h, 1, Theme.accent);
        g.drawString(font, Component.translatable("texturemixer.profile.name").getString(), x + 8, y + 8, Theme.text, false);
        Ui.rect(g, nameBox.getX() - 2, nameBox.getY() - 2, nameBox.getWidth() + 4, nameBox.getHeight() + 4, Theme.tile);
        nameBox.render(g, mx, my, 0);
        Ui.button(g, font, okBtn.x, okBtn.y, okBtn.w, okBtn.h, okBtn.label.get(), Ui.in(mx, my, okBtn.x, okBtn.y, okBtn.w, okBtn.h), true, true);
        Ui.button(g, font, cancelBtn.x, cancelBtn.y, cancelBtn.w, cancelBtn.h, cancelBtn.label.get(), Ui.in(mx, my, cancelBtn.x, cancelBtn.y, cancelBtn.w, cancelBtn.h), true, false);
    }

    // ------------------------------------------------------------------ Eingaben
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (creatingProfile) {
            if (button == 0 && Ui.in(mx, my, okBtn.x, okBtn.y, okBtn.w, okBtn.h)) { confirmProfile(); return true; }
            if (button == 0 && Ui.in(mx, my, cancelBtn.x, cancelBtn.y, cancelBtn.w, cancelBtn.h)) { creatingProfile = false; nameBox.visible = false; return true; }
            return super.mouseClicked(event, doubleClick);
        }
        if (super.mouseClicked(event, doubleClick)) return true;
        if (button == 0) {
            for (Btn b : buttons) if (!b.text && Ui.in(mx, my, b.x, b.y, b.w, b.h)) { b.action.run(); return true; }
            for (int i = 0; i < cats.size(); i++)
                if (Ui.in(mx, my, catX + i * (catSize + catGap), catY, catSize, catSize)) { setCategory(i); return true; }
        }
        if (Ui.in(mx, my, 0, gridY, width, gridH)) {
            for (int i = 0; i < entries.size(); i++) {
                int x = gridX + (i % cols) * (tileW + gap);
                int y = Math.round(gridY + (i / cols) * (tileH + gap) - scroll);
                if (Ui.in(mx, my, x, y, tileW, tileH)) {
                    if (button == 0) select(entries.get(i));
                    else if (button == 1) toggleFavorite(entries.get(i));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (creatingProfile) return true;
        if (Ui.in(mx, my, 0, catY, width, catSize)) { setCategory(catIndex + (sy > 0 ? -1 : 1)); return true; }
        scrollTarget -= (float) (sy * 46);
        if (!Theme.animations) scroll = scrollTarget;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (creatingProfile) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { confirmProfile(); return true; }
            if (key == GLFW.GLFW_KEY_ESCAPE) { creatingProfile = false; nameBox.visible = false; return true; }
            return super.keyPressed(event);
        }
        if (!searchBox.isFocused()) {
            if (key == GLFW.GLFW_KEY_LEFT) { setCategory(catIndex - 1); return true; }
            if (key == GLFW.GLFW_KEY_RIGHT) { setCategory(catIndex + 1); return true; }
        }
        if (key == GLFW.GLFW_KEY_F && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) { setFocused(searchBox); searchBox.setFocused(true); return true; }
        return super.keyPressed(event);
    }
}
