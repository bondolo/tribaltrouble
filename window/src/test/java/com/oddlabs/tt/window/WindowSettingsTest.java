package com.oddlabs.tt.window;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowSettingsTest {

    @Test
    void testDefaultValues() {
        WindowSettings settings = new WindowSettings();
        assertEquals(-1, settings.view_width);
        assertEquals(-1, settings.view_height);
        assertEquals(4, settings.view_samples);
        assertTrue(settings.fullscreen);
    }

    @Test
    void testPropertiesSerialization() {
        WindowSettings original = new WindowSettings();
        original.fullscreen = false;
        original.view_width = 1600;
        original.view_height = 900;
        original.view_samples = 2;

        Properties props = new Properties();
        original.saveToProperties(props);

        WindowSettings loaded = new WindowSettings();
        loaded.loadFromProperties(props);

        assertFalse(loaded.fullscreen);
        assertEquals(1600, loaded.view_width);
        assertEquals(900, loaded.view_height);
        assertEquals(2, loaded.view_samples);
    }
}
