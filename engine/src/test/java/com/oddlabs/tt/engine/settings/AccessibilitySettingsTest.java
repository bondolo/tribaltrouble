package com.oddlabs.tt.engine.settings;

import com.oddlabs.util.Color;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AccessibilitySettingsTest {

    @Test
    void testDefaultValues() {
        AccessibilitySettings settings = new AccessibilitySettings();
        assertEquals(0, settings.cvd_mode);
        assertEquals(1.0f, settings.cvd_intensity, 0.001f);
        assertFalse(settings.high_contrast);
        assertEquals(0.5f, settings.contrast_intensity, 0.001f);
        assertFalse(settings.invert_colours);
        assertTrue(settings.sound_emojis);
        assertNotNull(settings.team_colours);
        assertEquals(AccessibilitySettings.DEFAULT_TEAM_COLOURS.length, settings.team_colours.length);
        assertNotNull(settings.linear_team_colours);
    }

    @Test
    void testSaveAndLoadProperties() {
        AccessibilitySettings settings = new AccessibilitySettings();
        settings.cvd_mode = 2;
        settings.cvd_intensity = 0.8f;
        settings.high_contrast = true;
        settings.contrast_intensity = 0.75f;
        settings.invert_colours = true;
        settings.team_stencil = true;
        settings.sound_emojis = false;
        settings.team_colours[0] = new Color.Standard(0xFF_11_22_33);
        settings.updateLinearColors();

        Properties props = new Properties();
        settings.saveToProperties(props);

        AccessibilitySettings loaded = new AccessibilitySettings();
        loaded.loadFromProperties(props);

        assertEquals(2, loaded.cvd_mode);
        assertEquals(0.8f, loaded.cvd_intensity, 0.001f);
        assertTrue(loaded.high_contrast);
        assertEquals(0.75f, loaded.contrast_intensity, 0.001f);
        assertTrue(loaded.invert_colours);
        assertTrue(loaded.team_stencil);
        assertFalse(loaded.sound_emojis);
        assertEquals(0xFF_11_22_33, loaded.team_colours[0].toInt());
        assertEquals(Color.toLinear(loaded.team_colours[0].r()), loaded.linear_team_colours[0].r(), 0.001f);
    }
}
