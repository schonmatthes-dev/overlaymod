package dev.videooverlay.capture;

import dev.videooverlay.config.OverlayConfig;
import dev.videooverlay.render.OverlayRenderer;

import java.util.concurrent.CopyOnWriteArrayList;

public final class CaptureManager {
    public static final CopyOnWriteArrayList<CaptureSource> sources = new CopyOnWriteArrayList<>();
    private static Thread thread;

    public static void init() {
        for (OverlayConfig.SourceConfig c : OverlayConfig.get().sources) sources.add(new CaptureSource(c));
        thread = new Thread(CaptureManager::loop, "videooverlay-capture");
        thread.setDaemon(true);
        thread.start();
    }

    public static CaptureSource add() {
        OverlayConfig.SourceConfig c = new OverlayConfig.SourceConfig();
        c.name = "Video " + (sources.size() + 1);
        OverlayConfig.get().sources.add(c);
        CaptureSource s = new CaptureSource(c);
        sources.add(s);
        return s;
    }

    /** Nur vom Render-Thread aufrufen (gibt Textur frei). */
    public static void remove(CaptureSource s) {
        sources.remove(s);
        OverlayConfig.get().sources.remove(s.cfg);
        OverlayRenderer.forget(s);
    }

    private static void loop() {
        while (true) {
            long start = System.nanoTime();
            OverlayConfig cfg = OverlayConfig.get();
            if (!cfg.visible) {
                sleep(200);
                continue;
            }
            for (CaptureSource s : sources) s.tick(cfg.textureWidth);
            long frameMs = 1000L / Math.max(1, cfg.captureFps);
            long used = (System.nanoTime() - start) / 1_000_000L;
            sleep(Math.max(5, frameMs - used));
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
