package dev.videooverlay;

import dev.videooverlay.capture.CaptureManager;
import dev.videooverlay.capture.CaptureSource;
import dev.videooverlay.capture.WindowCapturer;
import dev.videooverlay.config.OverlayConfig;
import dev.videooverlay.gui.ConfigScreen;
import dev.videooverlay.render.OverlayRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoOverlayClient implements ClientModInitializer {
    public static final String MOD_ID = "videooverlay";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private KeyBinding openConfig, pauseAll, toggleOverlay;

    @Override
    public void onInitializeClient() {
        if (!WindowCapturer.isWindows()) {
            LOGGER.warn("Video Overlay funktioniert nur unter Windows - Mod ist inaktiv.");
            return;
        }
        OverlayConfig.load();
        CaptureManager.init();

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openConfig = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.videooverlay.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, cat));
        pauseAll = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.videooverlay.pause_all", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, cat));
        toggleOverlay = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.videooverlay.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, cat));

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "overlay"), OverlayRenderer::render);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openConfig.wasPressed()) mc.setScreen(new ConfigScreen(mc.currentScreen));
            while (toggleOverlay.wasPressed()) {
                OverlayConfig c = OverlayConfig.get();
                c.visible = !c.visible;
                OverlayConfig.save();
            }
            while (pauseAll.wasPressed()) togglePauseAll(mc);
        });
    }

    private static void togglePauseAll(MinecraftClient mc) {
        boolean anyRunning = CaptureManager.sources.stream().anyMatch(s -> !s.paused);
        for (CaptureSource s : CaptureManager.sources) s.paused = anyRunning;
        if (OverlayConfig.get().sendMediaKeyOnPause) WindowCapturer.sendPlayPause();
        mc.inGameHud.setOverlayMessage(Text.literal(anyRunning ? "Video Overlay: pausiert" : "Video Overlay: laeuft"), false);
    }
}
