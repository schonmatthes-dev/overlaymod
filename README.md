# Video Overlay (Fabric, Windows)

Zeigt offene Fenster (YouTube im Browser, TikTok Live, Discord-Stream) als Overlay im Spiel.

## Bedienung
- `O`  Einstellungen (Quelle waehlen/anlegen, Fenster zuweisen, Groesse, Position, Deckkraft, Pause)
- `P`  alle Quellen pausieren / fortsetzen
- Overlay ein/aus: Taste in den Steuerungs-Einstellungen belegen
- Config-Datei: `config/videooverlay.json` (z.B. `captureFps`, `textureWidth`, `sendMediaKeyOnPause`)

## Tipps
- Jedes Video in ein **eigenes Browserfenster** ziehen. Es wird immer nur das aktive Tab eines Fensters gezeigt.
- Discord: Stream im Discord-Fenster geoeffnet lassen (nicht minimieren).
- **Minimierte Fenster** liefern kein Bild. Verdeckte Fenster gehen meist; falls Chrome das Bild einfriert,
  wenn es komplett verdeckt ist, hilft der Start-Parameter `--disable-features=CalculateNativeWinOcclusion`
  oder ein zweiter Monitor.
- DRM-geschuetzte Inhalte (z.B. Netflix) werden schwarz.
- "Pause" friert das Overlay-Bild ein. Das Video selbst stoppt nur, wenn `sendMediaKeyOnPause` an ist
  (sendet die globale Play/Pause-Taste - bei mehreren Quellen trifft sie evtl. die falsche).

## Bauen
Versionen in `gradle.properties` aus sword-belt-v2 uebernehmen. Falls etwas nicht kompiliert:
alle versionsempfindlichen Rendering-Aufrufe stehen in `render/Gfx.java`, Keybinds in `VideoOverlayClient.java`.
