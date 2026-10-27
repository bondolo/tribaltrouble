package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.base.global.Settings;
import com.oddlabs.tt.window.SerializableDisplayMode;
import com.oddlabs.tt.window.Window;
import com.oddlabs.tt.window.WindowEventListener;
import com.oddlabs.tt.window.WindowSettings;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RenderHostMSAATest {

    private record MockWindow(
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

    private boolean originalDrawMsaa;

    @BeforeEach
    void setUp() {
        originalDrawMsaa = DebugFlags.draw_msaa;
        DebugFlags.draw_msaa = true;
    }

    @AfterEach
    void tearDown() {
        DebugFlags.draw_msaa = originalDrawMsaa;
    }

    @Test
    void testTier3HighDPIDisablesMSAA() {
        // 16" MBP at 3584x2240, 264 DPI (> 165 DPI)
        Window window = new MockWindow(3584, 2240, new Vector2f(344.5f, 215.3f), new Vector2f(2.0f, 2.0f), 1.0f, true);
        RenderHost host = new RenderHost(window, new Settings());
        assertEquals(0, host.computeEffectiveSamples(4));
    }

    @Test
    void testTier2BalancedDPIUses2xMSAA() {
        // 32" 4K at 3840x2160, 137.7 DPI (130 <= DPI <= 165)
        Window window = new MockWindow(3840, 2160, new Vector2f(708.4f, 398.5f), new Vector2f(1.0f, 1.0f), 1.0f, true);
        RenderHost host = new RenderHost(window, new Settings());
        assertEquals(2, host.computeEffectiveSamples(4));
    }

    @Test
    void testTier2HeightAbove1440Uses2xMSAAEvenWithLowDPI() {
        // 42" 4K TV at 3840x2160, 104.9 DPI (< 130 DPI, but height 2160 > 1440)
        Window window = new MockWindow(3840, 2160, new Vector2f(929.8f, 523.0f), new Vector2f(1.0f, 1.0f), 1.0f, true);
        RenderHost host = new RenderHost(window, new Settings());
        assertEquals(2, host.computeEffectiveSamples(4));
    }

    @Test
    void testTier1StandardDensityUses4xMSAA() {
        // 27" 1440p at 2560x1440, 108.8 DPI (< 130 DPI and height <= 1440)
        Window window = new MockWindow(2560, 1440, new Vector2f(597.7f, 336.2f), new Vector2f(1.0f, 1.0f), 1.0f, true);
        RenderHost host = new RenderHost(window, new Settings());
        assertEquals(4, host.computeEffectiveSamples(4));

        // 24" 1080p at 1920x1080, 91.8 DPI (< 130 DPI and height <= 1440)
        Window window1080 = new MockWindow(1920, 1080, new Vector2f(531.3f, 298.9f), new Vector2f(1.0f, 1.0f), 1.0f, true);
        RenderHost host1080 = new RenderHost(window1080, new Settings());
        assertEquals(4, host1080.computeEffectiveSamples(4));
    }

    @Test
    void testWindowedHighDensityDisablesMSAA() {
        // Windowed mode with OS scaling (density = 2.0f)
        Window window = new MockWindow(1792, 1120, new Vector2f(344.5f, 215.3f), new Vector2f(2.0f, 2.0f), 2.0f, false);
        RenderHost host = new RenderHost(window, new Settings());
        assertEquals(0, host.computeEffectiveSamples(4));
    }

    @Test
    void testUserRequestedZeroDisablesMSAA() {
        Window window = new MockWindow(1920, 1080, new Vector2f(531.3f, 298.9f), new Vector2f(1.0f, 1.0f), 1.0f, true);
        RenderHost host = new RenderHost(window, new Settings());
        assertEquals(0, host.computeEffectiveSamples(0));
    }

    @Test
    void testDebugFlagDrawMsaaOffDisablesMSAA() {
        DebugFlags.draw_msaa = false;
        Window window = new MockWindow(1920, 1080, new Vector2f(531.3f, 298.9f), new Vector2f(1.0f, 1.0f), 1.0f, true);
        RenderHost host = new RenderHost(window, new Settings());
        assertEquals(0, host.computeEffectiveSamples(4));
    }
}
