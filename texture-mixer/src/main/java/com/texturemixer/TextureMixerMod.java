package com.texturemixer;

import com.texturemixer.client.Features;
import com.texturemixer.client.McCompat;
import com.texturemixer.client.TextureMixerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class TextureMixerMod implements ClientModInitializer {
    public static final String MOD_ID = "texturemixer";
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        MixManager.init();
        Features.init();
        openKey = KeyBindingHelper.registerKeyBinding(McCompat.key("key.texturemixer.open", GLFW.GLFW_KEY_U));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                if (!(mc.screen instanceof TextureMixerScreen)) mc.setScreen(new TextureMixerScreen(mc.screen));
            }
            Features.tick(mc);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(Features::shutdown);
        ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
            Features.onSettingsChanged();
            Thread t = new Thread(() -> MixManager.library().packs(), "TextureMixer-Scan");   // Packs im Hintergrund einlesen
            t.setDaemon(true);
            t.start();
        });
    }
}
