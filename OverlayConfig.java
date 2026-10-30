package dev.videooverlay.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class OverlayConfig {

    /** Eine Quelle = ein Fenster, das im Spiel angezeigt wird. */
    public static final class SourceConfig {
        public String name = "Video";
        public String windowTitle = "";
        public boolean enabled = true;
        /** Position 0..1 (0 = links/oben, 1 = rechts/unten), Fenster bleibt immer komplett im Bild. */
        public float x = 0.98f, y = 0.05f;
        /** Breite als Anteil der Bildschirmbreite. */
        public float width = 0.30f;
        public float opacity = 1.0f;
    }

    public boolean visible = true;
    public int captureFps = 15;
    /** Breite, auf die jedes Bild vor dem Hochladen verkleinert wird (Performance). */
    public int textureWidth = 640;
    /** Beim Pausieren zusaetzlich die globale Play/Pause-Medientaste senden. */
    public boolean sendMediaKeyOnPause = false;
    public List<SourceConfig> sources = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static OverlayConfig instance;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("videooverlay.json");
    }

    public static OverlayConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) instance = GSON.fromJson(Files.readString(p), OverlayConfig.class);
        } catch (Exception ignored) {
        }
        if (instance == null) instance = new OverlayConfig();
        if (instance.sources == null) instance.sources = new ArrayList<>();
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(get()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
