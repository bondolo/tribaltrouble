package com.oddlabs.tt.window;

import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DisplayDPITest {

    private record TestWindow(
            int width,
            int height,
            Vector2f physicalSize,
            Vector2f contentScale,
            float pixelDensity,
            boolean exclusiveFullscreen
    ) implements Window {
        @Override public int getWidth() { return width; }
        @Override public int getHeight() { return height; }
        @Override public int getLogicalWidth() { return width; }
        @Override public int getLogicalHeight() { return height; }
        @Override public Vector2f getMonitorPhysicalSize() { return physicalSize; }
        @Override public Vector2f getMonitorContentScale() { return contentScale; }
        @Override public Vector2f getWindowContentScale() { return contentScale; }
        @Override public float getPixelDensity() { return pixelDensity; }
        @Override public boolean isExclusiveFullscreen() { return exclusiveFullscreen; }
        @Override public boolean isExclusiveFullscreenMode(SerializableDisplayMode mode) { return false; }
        @Override public List<SerializableDisplayMode> getFullscreenDisplayModes() { return List.of(); }
        @Override public List<SerializableDisplayMode> getWindowedDisplayModes() { return List.of(); }
        @Override public SerializableDisplayMode getDisplayMode() { return new SerializableDisplayMode(width, height, 32, 60); }
        @Override public void setDisplayMode(SerializableDisplayMode mode) {}
        @Override public void create(SerializableDisplayMode mode, boolean fullscreen) {}
        @Override public void update() {}
        @Override public void pollEvents() {}
        @Override public void waitEvents(int timeoutMs) {}
        @Override public void close() {}
        @Override public WindowSettings getSettings() { return new WindowSettings(); }
        @Override public void setSettings(WindowSettings settings) {}
        @Override public void setVSyncEnabled(boolean enabled) {}
        @Override public void setEventListener(@Nullable WindowEventListener listener) {}
        @Override public void setIcon(Path imagePath) {}
        @Override public void restore() {}
        @Override public void minimize() {}
        @Override public void show() {}
        @Override public void focus() {}
        @Override public void makeCurrent() {}
        @Override public boolean isFullscreen() { return exclusiveFullscreen; }
        @Override public void setFullscreen(boolean fullscreen) {}
        @Override public void setTitle(String title) {}
        @Override public boolean isCloseRequested() { return false; }
        @Override public void setCloseRequested(boolean value) {}
        @Override public boolean isActive() { return true; }
        @Override public boolean isVisible() { return true; }
        @Override public boolean isIconified() { return false; }
        @Override public boolean isMaximized() { return false; }
        @Override public boolean wasResized() { return false; }
        @Override public boolean isOpen() { return true; }
        @Override public void updateSystemUI(boolean playing) {}
        @Override public void showOpenFileDialog(String filterName, String filterPattern, Consumer<@Nullable Path> callback) {}
        @Override public void showSaveFileDialog(String filterName, String filterPattern, @Nullable String defaultLocation, Consumer<@Nullable Path> callback) {}
    }

    @Test
    void testMacBookPro16RetinaDPI() {
        Window window = new TestWindow(3584, 2240, new Vector2f(344.5f, 215.3f), new Vector2f(2.0f, 2.0f), 1.0f, true);
        float dpi = window.getDisplayDPI();
        assertEquals(264.3f, dpi, 1.0f);
    }

    @Test
    void testDisplay4K32InchDPI() {
        Window window = new TestWindow(3840, 2160, new Vector2f(708.4f, 398.5f), new Vector2f(1.0f, 1.0f), 1.0f, true);
        float dpi = window.getDisplayDPI();
        assertEquals(137.7f, dpi, 1.0f);
    }

    @Test
    void testDisplay4K27InchDPI() {
        Window window = new TestWindow(3840, 2160, new Vector2f(597.7f, 336.2f), new Vector2f(1.0f, 1.0f), 1.0f, true);
        float dpi = window.getDisplayDPI();
        assertEquals(163.1f, dpi, 1.0f);
    }

    @Test
    void testDisplay1440p27InchDPI() {
        Window window = new TestWindow(2560, 1440, new Vector2f(597.7f, 336.2f), new Vector2f(1.0f, 1.0f), 1.0f, true);
        float dpi = window.getDisplayDPI();
        assertEquals(108.8f, dpi, 1.0f);
    }

    @Test
    void testDisplay1080p24InchDPI() {
        Window window = new TestWindow(1920, 1080, new Vector2f(531.3f, 298.9f), new Vector2f(1.0f, 1.0f), 1.0f, true);
        float dpi = window.getDisplayDPI();
        assertEquals(91.8f, dpi, 1.0f);
    }

    @Test
    void testFallbackWhenPhysicalDimensionsUnavailable() {
        Window window = new TestWindow(1920, 1080, new Vector2f(0f, 0f), new Vector2f(1.5f, 1.5f), 1.0f, true);
        float dpi = window.getDisplayDPI();
        assertEquals(144.0f, dpi, 0.01f);
    }
}
