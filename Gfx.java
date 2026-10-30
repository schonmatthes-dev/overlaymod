package dev.videooverlay.render;

import dev.videooverlay.capture.WindowCapturer.Frame;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Alle Minecraft-Rendering-Aufrufe, die sich zwischen Versionen gern aendern, liegen hier.
 * Falls beim Kompilieren etwas nicht passt, ist es sehr wahrscheinlich eine Zeile in dieser Datei.
 */
final class Gfx {
    static final class Tex {
        Identifier id;
        NativeImageBackedTexture texture;
        int w, h;
    }

    private static int counter;

    static Tex upload(Tex old, Frame f) {
        if (old == null || old.w != f.width() || old.h != f.height()) {
            release(old);
            old = create(f.width(), f.height());
        }
        NativeImage img = old.texture.getImage();
        int[] px = f.argb();
        for (int y = 0; y < f.height(); y++) {
            for (int x = 0; x < f.width(); x++) {
                img.setColorArgb(x, y, px[y * f.width() + x]);
            }
        }
        old.texture.upload();
        return old;
    }

    private static Tex create(int w, int h) {
        Tex t = new Tex();
        t.w = w;
        t.h = h;
        t.id = Identifier.of("videooverlay", "source/" + (counter++));
        t.texture = new NativeImageBackedTexture("videooverlay_" + t.id.getPath(), w, h, false);
        MinecraftClient.getInstance().getTextureManager().registerTexture(t.id, t.texture);
        return t;
    }

    static void release(Tex t) {
        if (t == null) return;
        MinecraftClient.getInstance().getTextureManager().destroyTexture(t.id);
    }

    static void draw(DrawContext ctx, Tex t, int x, int y, int w, int h, float opacity) {
        int a = Math.max(0, Math.min(255, (int) (opacity * 255)));
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, t.id, x, y, 0f, 0f, w, h, t.w, t.h, t.w, t.h, (a << 24) | 0xFFFFFF);
    }
}
