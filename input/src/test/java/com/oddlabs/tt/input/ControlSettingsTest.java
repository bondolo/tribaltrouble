package com.oddlabs.tt.input;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlSettingsTest {

    @Test
    void testDefaultValues() {
        ControlSettings settings = new ControlSettings();
        assertFalse(settings.invert_camera_pitch);
        assertFalse(settings.aggressive_units);
        assertEquals(0.5f, settings.mapmode_delay);
        assertEquals(0.5f, settings.tooltip_delay);
        assertEquals(0.0f, settings.ui_scale);
        assertEquals("default", settings.language);
    }

    @Test
    void testPropertiesSerialization() {
        ControlSettings original = new ControlSettings();
        original.invert_camera_pitch = true;
        original.aggressive_units = true;
        original.mapmode_delay = 1.0f;
        original.tooltip_delay = 0.25f;
        original.ui_scale = 0.5f;
        original.language = "fr";

        Properties props = new Properties();
        original.saveToProperties(props);

        ControlSettings loaded = new ControlSettings();
        loaded.loadFromProperties(props);

        assertTrue(loaded.invert_camera_pitch);
        assertTrue(loaded.aggressive_units);
        assertEquals(1.0f, loaded.mapmode_delay);
        assertEquals(0.25f, loaded.tooltip_delay);
        assertEquals(0.5f, loaded.ui_scale);
        assertEquals("fr", loaded.language);
    }
}
