package dev.videooverlay.capture;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.util.ArrayList;
import java.util.List;

/** Greift den Inhalt beliebiger Windows-Fenster ab (auch wenn sie verdeckt sind) per PrintWindow. */
public final class WindowCapturer {

    public interface User32X extends StdCallLibrary {
        User32X I = Native.load("user32", User32X.class, W32APIOptions.DEFAULT_OPTIONS);
        boolean PrintWindow(HWND hwnd, HDC hdc, int flags);
        boolean IsIconic(HWND hwnd);
        boolean IsWindow(HWND hwnd);
        boolean GetClientRect(HWND hwnd, RECT rect);
        void keybd_event(byte vk, byte scan, int flags, Pointer extra);
    }

    public record WindowInfo(HWND hwnd, String title) {}
    public record Frame(int width, int height, int[] argb) {}

    private static final int PW_CLIENTONLY = 1;
    private static final int PW_RENDERFULLCONTENT = 2;

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static List<WindowInfo> listWindows() {
        List<WindowInfo> out = new ArrayList<>();
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) return true;
            char[] buf = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, buf, 512);
            String title = Native.toString(buf);
            if (title.isBlank() || title.startsWith("Minecraft")) return true;
            out.add(new WindowInfo(hwnd, title));
            return true;
        }, Pointer.NULL);
        return out;
    }

    public static boolean isValid(HWND h) {
        return h != null && User32X.I.IsWindow(h);
    }

    /** Sucht ein Fenster: erst exakter Titel, dann "enthaelt". */
    public static HWND find(String hint) {
        if (hint == null || hint.isBlank()) return null;
        String low = hint.toLowerCase();
        HWND partial = null;
        for (WindowInfo w : listWindows()) {
            if (w.title().equals(hint)) return w.hwnd();
            if (partial == null && w.title().toLowerCase().contains(low)) partial = w.hwnd();
        }
        return partial;
    }

    public static void sendPlayPause() {
        final byte VK_MEDIA_PLAY_PAUSE = (byte) 0xB3;
        User32X.I.keybd_event(VK_MEDIA_PLAY_PAUSE, (byte) 0, 0, null);
        User32X.I.keybd_event(VK_MEDIA_PLAY_PAUSE, (byte) 0, 2, null); // KEYEVENTF_KEYUP
    }

    /** Liefert das Fenster-Bild (Client-Bereich), auf maxWidth verkleinert. null = nicht moeglich (z.B. minimiert). */
    public static Frame capture(HWND hwnd, int maxWidth) {
        HDC screen = null, mem = null;
        HBITMAP bmp = null;
        HANDLE old = null;
        Memory buf = null;
        try {
            if (User32X.I.IsIconic(hwnd)) return null;
            RECT r = new RECT();
            if (!User32X.I.GetClientRect(hwnd, r)) return null;
            int w = r.right - r.left, h = r.bottom - r.top;
            if (w <= 0 || h <= 0) return null;

            screen = User32.INSTANCE.GetDC(null);
            mem = GDI32.INSTANCE.CreateCompatibleDC(screen);
            bmp = GDI32.INSTANCE.CreateCompatibleBitmap(screen, w, h);
            old = GDI32.INSTANCE.SelectObject(mem, bmp);

            if (!User32X.I.PrintWindow(hwnd, mem, PW_CLIENTONLY | PW_RENDERFULLCONTENT)) return null;

            BITMAPINFO bi = new BITMAPINFO();
            bi.bmiHeader.biSize = bi.bmiHeader.size();
            bi.bmiHeader.biWidth = w;
            bi.bmiHeader.biHeight = -h; // top-down
            bi.bmiHeader.biPlanes = 1;
            bi.bmiHeader.biBitCount = 32;
            bi.bmiHeader.biCompression = WinGDI.BI_RGB;

            buf = new Memory((long) w * h * 4);
            GDI32.INSTANCE.GetDIBits(mem, bmp, 0, h, buf, bi, WinGDI.DIB_RGB_COLORS);
            int[] px = buf.getIntArray(0, w * h);

            int tw = Math.min(w, Math.max(64, maxWidth));
            int th = Math.max(1, h * tw / w);
            int[] out = new int[tw * th];
            for (int y = 0; y < th; y++) {
                int sy = y * h / th;
                for (int x = 0; x < tw; x++) {
                    out[y * tw + x] = px[sy * w + x * w / tw] | 0xFF000000;
                }
            }
            return new Frame(tw, th, out);
        } catch (Throwable t) {
            return null;
        } finally {
            try {
                if (mem != null && old != null) GDI32.INSTANCE.SelectObject(mem, old);
                if (bmp != null) GDI32.INSTANCE.DeleteObject(bmp);
                if (mem != null) GDI32.INSTANCE.DeleteDC(mem);
                if (screen != null) User32.INSTANCE.ReleaseDC(null, screen);
                if (buf != null) buf.dispose();
            } catch (Throwable ignored) {
            }
        }
    }
}
