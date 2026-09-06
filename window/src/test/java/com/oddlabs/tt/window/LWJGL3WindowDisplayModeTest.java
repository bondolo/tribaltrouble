package com.oddlabs.tt.window;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests display mode filtering, deduplication by resolution, and refresh rate selection.
 */
final class LWJGL3WindowDisplayModeTest {

    @Test
    void testPrefersHighestRefreshRate() {
        var mode60 = new SerializableDisplayMode(1920, 1080, 32, 60);
        var mode120 = new SerializableDisplayMode(1920, 1080, 32, 120);
        var mode144 = new SerializableDisplayMode(1920, 1080, 32, 144);

        List<SerializableDisplayMode> result = LWJGL3Window.filterAndSortModes(List.of(mode120, mode60, mode144));
        assertEquals(1, result.size());
        assertEquals(144, result.getFirst().getFrequency());
    }

    @Test
    void testPrefersHigherBitDepthWhenFrequencyMatches() {
        var mode16 = new SerializableDisplayMode(1280, 720, 16, 60);
        var mode32 = new SerializableDisplayMode(1280, 720, 32, 60);

        List<SerializableDisplayMode> result = LWJGL3Window.filterAndSortModes(List.of(mode16, mode32));
        assertEquals(1, result.size());
        assertEquals(32, result.getFirst().getBitsPerPixel());
    }

    @Test
    void testSortOrderWidthThenHeightDescending() {
        var mode4k = new SerializableDisplayMode(3840, 2160, 32, 60);
        var mode1080p = new SerializableDisplayMode(1920, 1080, 32, 60);
        var mode1440p = new SerializableDisplayMode(2560, 1440, 32, 60);
        var modeUltraWide = new SerializableDisplayMode(2560, 1080, 32, 60);

        List<SerializableDisplayMode> result = LWJGL3Window.filterAndSortModes(
                List.of(mode1080p, mode4k, modeUltraWide, mode1440p));

        assertEquals(4, result.size());
        assertEquals(3840, result.get(0).getWidth());
        assertEquals(2560, result.get(1).getWidth());
        assertEquals(1440, result.get(1).getHeight());
        assertEquals(2560, result.get(2).getWidth());
        assertEquals(1080, result.get(2).getHeight());
        assertEquals(1920, result.get(3).getWidth());
    }

    @Test
    void testMacNativeModesDoNotExceedNativeDimensions() {
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) return;
        List<SerializableDisplayMode> modes = LWJGL3Window.getMacNativeDisplayModes();
        if (modes == null || modes.isEmpty()) return;

        // Modes returned by getMacNativeDisplayModes are not yet filtered by filterAndSortModes
        // Ensure that filterAndSortModes also works cleanly on them
        List<SerializableDisplayMode> filtered = LWJGL3Window.filterAndSortModes(modes);
        assertFalse(filtered.isEmpty());
        // Max mode is the first in filtered list
        SerializableDisplayMode maxMode = filtered.getFirst();
        for (SerializableDisplayMode mode : filtered) {
            assertTrue(mode.getWidth() <= maxMode.getWidth());
            assertTrue(mode.getHeight() <= maxMode.getHeight());
            assertTrue(mode.getFrequency() > 0);
        }
    }
}
