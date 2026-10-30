package dev.videooverlay.capture;

import com.sun.jna.platform.win32.WinDef.HWND;
import dev.videooverlay.config.OverlayConfig.SourceConfig;

import java.util.concurrent.atomic.AtomicReference;

public final class CaptureSource {
    public final SourceConfig cfg;
    public volatile boolean paused;

    private volatile HWND hwnd;
    private volatile boolean windowFound;
    private long lastResolve;
    private final AtomicReference<WindowCapturer.Frame> pending = new AtomicReference<>();

    public CaptureSource(SourceConfig cfg) {
        this.cfg = cfg;
    }

    public boolean isWindowFound() {
        return windowFound;
    }

    public void bind(HWND h, String title) {
        this.hwnd = h;
        this.cfg.windowTitle = title;
        this.pending.set(null);
    }

    /** Wird vom Capture-Thread aufgerufen. */
    void tick(int maxWidth) {
        if (!cfg.enabled || paused) return;
        HWND h = hwnd;
        long now = System.currentTimeMillis();
        if (!WindowCapturer.isValid(h) && now - lastResolve > 2000) {
            lastResolve = now;
            h = WindowCapturer.find(cfg.windowTitle);
            hwnd = h;
        }
        windowFound = WindowCapturer.isValid(h);
        if (!windowFound) return;
        WindowCapturer.Frame f = WindowCapturer.capture(h, maxWidth);
        if (f != null) pending.set(f);
    }

    /** Wird vom Render-Thread aufgerufen: neuestes Bild oder null. */
    public WindowCapturer.Frame takeFrame() {
        return pending.getAndSet(null);
    }
}
