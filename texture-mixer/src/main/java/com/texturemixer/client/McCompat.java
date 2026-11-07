package com.texturemixer.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * ALLE versionsabhängigen Minecraft-Aufrufe (Rendering, Texturen, Sounds, Identifier) sind hier gebündelt.
 * Bei einer anderen Minecraft-Version muss in der Regel nur diese Datei angepasst werden.
 */
public final class McCompat {
    private McCompat() {}

    public static Identifier id(String ns, String path) { return Identifier.fromNamespaceAndPath(ns, path); }
    public static Identifier parse(String s) { return Identifier.parse(s); }

    public static KeyMapping.Category keyCategory() { return KeyMapping.Category.register(id("texturemixer", "main")); }
    public static KeyMapping key(String name, int glfwKey) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, glfwKey, keyCategory());
    }

    public static ItemStack icon(String itemId) {
        return BuiltInRegistries.ITEM.getOptional(parse(itemId)).map(ItemStack::new).orElse(new ItemStack(Items.BARRIER));
    }

    /** Zeichnet die komplette Textur (tw x th Pixel) skaliert in ein dw x dh großes Rechteck. */
    public static void blit(GuiGraphics g, Identifier tex, int x, int y, int dw, int dh, int tw, int th) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, dw, dh, tw, th, tw, th);
    }

    public static void drawScaled(GuiGraphics g, net.minecraft.client.gui.Font font, String text, float x, float y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.drawString(font, text, 0, 0, color, true);
        g.pose().popMatrix();
    }

    public static void pushScale(GuiGraphics g, float x, float y, float scale) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
    }
    public static void pop(GuiGraphics g) { g.pose().popMatrix(); }

    public static void registerTexture(Identifier id, NativeImage img) {
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, img));
    }
    public static void releaseTexture(Identifier id) { Minecraft.getInstance().getTextureManager().release(id); }

    /** Liest eine Datei aus dem niedrigsten Pack (= Vanilla) des aktuellen ResourceManagers. Pfad z. B. "assets/minecraft/textures/item/x.png". */
    public static byte[] readVanilla(String assetPath) {
        String rel = assetPath.substring("assets/".length());
        int s = rel.indexOf('/');
        List<Resource> stack = Minecraft.getInstance().getResourceManager().getResourceStack(id(rel.substring(0, s), rel.substring(s + 1)));
        if (stack.isEmpty()) return null;
        try (InputStream in = stack.get(0).open()) { return in.readAllBytes(); }
        catch (IOException e) { return null; }
    }

    // ---- Sounds
    public static SoundEvent sound(String name) {
        return switch (name) {
            case "click" -> SoundEvents.UI_BUTTON_CLICK.value();
            case "ding" -> SoundEvents.EXPERIENCE_ORB_PICKUP;
            case "bell" -> SoundEvents.NOTE_BLOCK_BELL.value();
            case "pling" -> SoundEvents.NOTE_BLOCK_PLING.value();
            case "crit" -> SoundEvents.PLAYER_ATTACK_CRIT;
            case "anvil" -> SoundEvents.ANVIL_LAND;
            case "thunder" -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            default -> SoundEvents.UI_BUTTON_CLICK.value();
        };
    }
    public static void playUi(String name, float pitch, float volume) {
        if (volume <= 0f) return;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound(name), pitch, volume));
    }
}
