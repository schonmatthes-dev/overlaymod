package dev.videooverlay.render;

import dev.videooverlay.capture.CaptureManager;
import dev.videooverlay.capture.CaptureSource;
import dev.videooverlay.capture.WindowCapturer.Frame;
import dev.videooverlay.config.OverlayConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.IdentityHashMap;
import java.util.Map;

public final class OverlayRenderer {
    private static final Map<CaptureSource, Gfx.Tex> TEX = new IdentityHashMap<>();

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!OverlayConfig.get().visible) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;

        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();

        for (CaptureSource s : CaptureManager.sources) {
            if (!s.cfg.enabled) continue;

            Gfx.Tex t = TEX.get(s);
            Frame f = s.takeFrame();
            if (f != null) {
                t = Gfx.upload(t, f);
                TEX.put(s, t);
            }

            int dw = Math.max(16, (int) (sw * s.cfg.width));
            int dh = t != null ? dw * t.h / t.w : dw * 9 / 16;
            int x = Math.max(0, (int) ((sw - dw) * s.cfg.x));
            int y = Math.max(0, (int) ((sh - dh) * s.cfg.y));

            if (t != null) {
                Gfx.draw(ctx, t, x, y, dw, dh, s.cfg.opacity);
            } else {
                ctx.fill(x, y, x + dw, y + dh, 0xAA000000);
            }
            if (!s.isWindowFound()) {
                ctx.drawText(mc.textRenderer, "Fenster nicht gefunden", x + 4, y + 4, 0xFFFF5555, true);
            } else if (s.paused) {
                ctx.drawText(mc.textRenderer, "|| Pause", x + 4, y + 4, 0xFFFFFFFF, true);
            }
        }
    }

    public static void forget(CaptureSource s) {
        Gfx.release(TEX.remove(s));
    }
}
