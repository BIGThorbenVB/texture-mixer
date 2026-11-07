package com.texturemixer.client;

import com.texturemixer.MixManager;
import com.texturemixer.core.FeatureRegistry;
import com.texturemixer.core.MixerConfig;

/** Menü-Sounds, gesteuert über die Einstellungen (Master Volume, GUI Sounds, Click, Hover). */
final class UiSounds {
    private UiSounds() {}

    static float master(MixerConfig c) { return (float) (FeatureRegistry.get("master_volume").num(c, "value") / 100.0); }

    static void click() {
        MixerConfig c = MixManager.config();
        if (FeatureRegistry.get("gui_sounds").enabled(c) && FeatureRegistry.get("click_sound").enabled(c))
            McCompat.playUi("click", 1.0f, master(c));
    }

    static void hover() {
        MixerConfig c = MixManager.config();
        if (FeatureRegistry.get("gui_sounds").enabled(c) && FeatureRegistry.get("hover_sound").enabled(c))
            McCompat.playUi("click", 2.0f, master(c) * 0.25f);
    }
}
