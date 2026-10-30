package dev.videooverlay.gui;

import dev.videooverlay.capture.CaptureManager;
import dev.videooverlay.capture.CaptureSource;
import dev.videooverlay.capture.WindowCapturer;
import dev.videooverlay.capture.WindowCapturer.WindowInfo;
import dev.videooverlay.config.OverlayConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.DoubleConsumer;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private int cur;
    private List<WindowInfo> windows = List.of();

    public ConfigScreen(Screen parent) {
        super(Text.literal("Video Overlay"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        windows = WindowCapturer.listWindows();
        build();
    }

    private CaptureSource current() {
        List<CaptureSource> l = CaptureManager.sources;
        return l.isEmpty() ? null : l.get(Math.max(0, Math.min(cur, l.size() - 1)));
    }

    private void build() {
        clearChildren();
        List<CaptureSource> list = CaptureManager.sources;
        int w = 220, x = width / 2 - w / 2, y = 20, step = 24;
        if (!list.isEmpty()) cur = Math.max(0, Math.min(cur, list.size() - 1));
        CaptureSource s = current();

        if (s != null) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                cur = (cur - 1 + list.size()) % list.size();
                build();
            }).dimensions(x, y, 20, 20).build());
            ButtonWidget label = ButtonWidget.builder(Text.literal("Quelle " + (cur + 1) + "/" + list.size()), b -> {
            }).dimensions(x + 22, y, w - 44, 20).build();
            label.active = false;
            addDrawableChild(label);
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                cur = (cur + 1) % list.size();
                build();
            }).dimensions(x + w - 20, y, 20, 20).build());
            y += step;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("+ Neue Quelle"), b -> {
            CaptureManager.add();
            cur = CaptureManager.sources.size() - 1;
            build();
        }).dimensions(x, y, s != null ? w / 2 - 2 : w, 20).build());
        if (s != null) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Loeschen"), b -> {
                CaptureManager.remove(s);
                build();
            }).dimensions(x + w / 2 + 2, y, w / 2 - 2, 20).build());
        }
        y += step;

        if (s != null) {
            String t = s.cfg.windowTitle.isBlank() ? "(keins gewaehlt)" : s.cfg.windowTitle;
            if (t.length() > 26) t = t.substring(0, 26) + "...";
            addDrawableChild(ButtonWidget.builder(Text.literal("Fenster: " + t), b -> {
                windows = WindowCapturer.listWindows();
                if (windows.isEmpty()) return;
                int idx = -1;
                for (int i = 0; i < windows.size(); i++) {
                    if (windows.get(i).title().equals(s.cfg.windowTitle)) idx = i;
                }
                WindowInfo next = windows.get((idx + 1) % windows.size());
                s.bind(next.hwnd(), next.title());
                build();
            }).dimensions(x, y, w, 20).build());
            y += step;

            addDrawableChild(new Slider(x, y, w, "Groesse", 0.05, 1.0, s.cfg.width, v -> s.cfg.width = (float) v));
            y += step;
            addDrawableChild(new Slider(x, y, w, "Position X", 0.0, 1.0, s.cfg.x, v -> s.cfg.x = (float) v));
            y += step;
            addDrawableChild(new Slider(x, y, w, "Position Y", 0.0, 1.0, s.cfg.y, v -> s.cfg.y = (float) v));
            y += step;
            addDrawableChild(new Slider(x, y, w, "Deckkraft", 0.1, 1.0, s.cfg.opacity, v -> s.cfg.opacity = (float) v));
            y += step;

            addDrawableChild(ButtonWidget.builder(Text.literal(s.cfg.enabled ? "Anzeige: an" : "Anzeige: aus"), b -> {
                s.cfg.enabled = !s.cfg.enabled;
                build();
            }).dimensions(x, y, w / 2 - 2, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(s.paused ? "Fortsetzen" : "Pausieren"), b -> {
                s.paused = !s.paused;
                if (OverlayConfig.get().sendMediaKeyOnPause) WindowCapturer.sendPlayPause();
                build();
            }).dimensions(x + w / 2 + 2, y, w / 2 - 2, 20).build());
            y += step;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Fertig"), b -> close())
                .dimensions(x, Math.min(y + 6, height - 26), w, 20).build());
    }

    /** Kein Blur-Hintergrund, damit man die Aenderungen live im Overlay sieht. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void close() {
        OverlayConfig.save();
        if (client != null) client.setScreen(parent);
    }

    private static final class Slider extends SliderWidget {
        private final String label;
        private final double min, max;
        private final DoubleConsumer onChange;

        Slider(int x, int y, int w, String label, double min, double max, double val, DoubleConsumer onChange) {
            super(x, y, w, 20, Text.empty(), (val - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            setMessage(Text.literal(label + ": " + Math.round((min + (max - min) * value) * 100) + " %"));
        }

        @Override
        protected void applyValue() {
            onChange.accept(min + (max - min) * value);
        }
    }
}
