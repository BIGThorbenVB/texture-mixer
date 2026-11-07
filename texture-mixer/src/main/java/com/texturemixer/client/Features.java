package com.texturemixer.client;

import com.texturemixer.MixManager;
import com.texturemixer.core.Feature;
import com.texturemixer.core.FeatureRegistry;
import com.texturemixer.core.MixerConfig;
import com.texturemixer.mixin.OptionInstanceAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Laufzeit-Logik aller umgesetzten Einstellungen (alles nur clientseitig). */
public final class Features {
    private static final int[] COLORS = {0xFFFFFFFF, 0xFF55FF55, 0xFFFF5555, 0xFFFFFF55, 0xFF55FFFF, 0xFFB070FF, 0xFFFFAA00};
    private static Double originalGamma;

    private Features() {}

    public static void init() {
        HudElementRegistry.addLast(McCompat.id("texturemixer", "overlay"), (g, tracker) -> renderHud(g));
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide() && entity instanceof LivingEntity && player == Minecraft.getInstance().player) onHit(entity);
            return InteractionResult.PASS;
        });
    }

    private static MixerConfig cfg() { return MixManager.config(); }
    private static Feature f(String id) { return FeatureRegistry.get(id); }

    // ------------------------------------------------------------------ Tick: Fullbright
    public static void tick(Minecraft mc) {
        if (mc.options == null) return;
        MixerConfig c = cfg();
        Feature fb = f("fullbright");
        OptionInstance<Double> gamma = mc.options.gamma();
        if (fb.enabled(c)) {
            if (originalGamma == null) originalGamma = gamma.get();
            double target = fb.num(c, "level");
            if (Math.abs(gamma.get() - target) > 1e-6) ((OptionInstanceAccessor<Double>) (Object) gamma).texturemixer$setValue(target);
        } else if (originalGamma != null) {
            gamma.set(originalGamma);
            originalGamma = null;
        }
    }

    /** Beim Beenden den echten Gamma-Wert zurückschreiben, damit options.txt nicht "10.0" speichert. */
    public static void shutdown(Minecraft mc) {
        if (originalGamma != null && mc.options != null) { mc.options.gamma().set(originalGamma); originalGamma = null; }
    }

    /** Nach jeder Änderung im Settings-Menü aufgerufen: wirkt sofort. */
    public static void onSettingsChanged() {
        Theme.refresh();
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        MixerConfig c = cfg();
        if (c.settings.containsKey("particles.mode")) {
            int m = f("particles").index(c, "mode");
            mc.options.particles().set(m == 0 ? ParticleStatus.ALL : m == 1 ? ParticleStatus.DECREASED : ParticleStatus.MINIMAL);
        }
        tick(mc);
    }

    // ------------------------------------------------------------------ Treffer-Effekte
    private static void onHit(net.minecraft.world.entity.Entity target) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        MixerConfig c = cfg();
        RandomSource r = level.getRandom();
        Vec3 p = target.position().add(0, target.getBbHeight() * 0.6, 0);

        Feature hc = f("hit_color");
        if (hc.enabled(c)) {
            int rgb = ((int) hc.num(c, "r") << 16) | ((int) hc.num(c, "g") << 8) | (int) hc.num(c, "b");
            ParticleOptions dust = new DustParticleOptions(rgb, (float) hc.num(c, "size"));
            for (int i = 0; i < 12; i++) level.addParticle(dust, p.x + rnd(r, 0.4), p.y + rnd(r, 0.3), p.z + rnd(r, 0.4), rnd(r, 0.1), rnd(r, 0.1), rnd(r, 0.1));
        }
        Feature hp = f("hit_particles");
        if (hp.enabled(c)) {
            ParticleOptions o = switch (hp.index(c, "style")) {
                case 1 -> ParticleTypes.FIREWORK;
                case 2 -> ParticleTypes.FLAME;
                case 3 -> ParticleTypes.HEART;
                case 4 -> ParticleTypes.SMOKE;
                default -> ParticleTypes.CRIT;
            };
            int n = (int) hp.num(c, "amount");
            for (int i = 0; i < n; i++) level.addParticle(o, p.x + rnd(r, 0.4), p.y + rnd(r, 0.3), p.z + rnd(r, 0.4), rnd(r, 0.25), rnd(r, 0.25) + 0.1, rnd(r, 0.25));
        }
        Feature ce = f("custom_hit_effects");
        if (ce.enabled(c)) {
            float vol = (float) (ce.num(c, "volume") / 100.0) * UiSounds.master(c);
            switch (ce.index(c, "effect")) {
                case 1 -> { for (int i = 0; i < 20; i++) level.addParticle(ParticleTypes.TOTEM_OF_UNDYING, p.x + rnd(r, 0.3), p.y + rnd(r, 0.3), p.z + rnd(r, 0.3), rnd(r, 0.4), rnd(r, 0.4) + 0.2, rnd(r, 0.4)); }
                case 2 -> { for (int i = 0; i < 16; i++) level.addParticle(i % 2 == 0 ? ParticleTypes.SOUL : ParticleTypes.SOUL_FIRE_FLAME, p.x + rnd(r, 0.3), p.y + rnd(r, 0.3), p.z + rnd(r, 0.3), rnd(r, 0.05), 0.08, rnd(r, 0.05)); }
                default -> {
                    for (int i = 0; i < 40; i++) level.addParticle(ParticleTypes.ELECTRIC_SPARK, p.x + rnd(r, 0.15), p.y + i * 0.1, p.z + rnd(r, 0.15), rnd(r, 0.2), rnd(r, 0.2), rnd(r, 0.2));
                    McCompat.playUi("thunder", 1.2f, vol);
                }
            }
        }
        Feature hs = f("hit_sound");
        if (hs.enabled(c)) {
            String[] names = {"ding", "bell", "pling", "crit", "anvil"};
            McCompat.playUi(names[Math.floorMod(hs.index(c, "sound"), names.length)], (float) hs.num(c, "pitch"),
                    (float) (hs.num(c, "volume") / 100.0) * UiSounds.master(c));
        }
    }

    private static double rnd(RandomSource r, double range) { return (r.nextDouble() - 0.5) * 2 * range; }

    // ------------------------------------------------------------------ HUD: FPS, Ping, Keystrokes
    private static void renderHud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        MixerConfig c = cfg();
        int sw = g.guiWidth(), sh = g.guiHeight();

        Feature fps = f("fps_display");
        if (fps.enabled(c)) drawLabel(g, mc, c, fps, mc.getFps() + " FPS", sw, sh);

        Feature ping = f("ping_display");
        if (ping.enabled(c)) {
            String t = "-- ms";
            if (mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info != null && !mc.hasSingleplayerServer()) t = info.getLatency() + " ms";
            }
            drawLabel(g, mc, c, ping, t, sw, sh);
        }

        Feature ks = f("keystrokes");
        if (ks.enabled(c)) {
            float s = (float) (ks.num(c, "scale") / 100.0);
            int color = COLORS[Math.floorMod(ks.index(c, "color"), COLORS.length)];
            float ox = (float) (ks.num(c, "x") / 100.0 * sw), oy = (float) (ks.num(c, "y") / 100.0 * sh);
            McCompat.pushScale(g, ox, oy, s);
            key(g, mc, "W", mc.options.keyUp.isDown(), 20, 0, 18, 18, color);
            key(g, mc, "A", mc.options.keyLeft.isDown(), 0, 20, 18, 18, color);
            key(g, mc, "S", mc.options.keyDown.isDown(), 20, 20, 18, 18, color);
            key(g, mc, "D", mc.options.keyRight.isDown(), 40, 20, 18, 18, color);
            key(g, mc, "LMB", mc.options.keyAttack.isDown(), 0, 40, 28, 18, color);
            key(g, mc, "RMB", mc.options.keyUse.isDown(), 30, 40, 28, 18, color);
            key(g, mc, "SPACE", mc.options.keyJump.isDown(), 0, 60, 58, 14, color);
            McCompat.pop(g);
        }
    }

    private static void drawLabel(GuiGraphics g, Minecraft mc, MixerConfig c, Feature f, String text, int sw, int sh) {
        float s = (float) (f.num(c, "scale") / 100.0);
        int color = COLORS[Math.floorMod(f.index(c, "color"), COLORS.length)];
        McCompat.drawScaled(g, mc.font, text, (float) (f.num(c, "x") / 100.0 * sw), (float) (f.num(c, "y") / 100.0 * sh), s, color);
    }

    private static void key(GuiGraphics g, Minecraft mc, String label, boolean down, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, down ? 0xB0FFFFFF : 0x80000000);
        g.drawString(mc.font, label, x + (w - mc.font.width(label)) / 2, y + (h - 8) / 2 + 1, down ? 0xFF000000 : color, false);
    }
}
