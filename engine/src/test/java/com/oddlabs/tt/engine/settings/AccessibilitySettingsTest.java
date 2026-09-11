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
        assertNotNull(settings.player_colours);
        assertEquals(AccessibilitySettings.DEFAULT_PLAYER_COLOURS.length, settings.player_colours.length);
        assertNotNull(settings.linear_player_colours);
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
        settings.player_colours[0] = new Color.Standard(0xFF_11_22_33);
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
        assertEquals(0xFF_11_22_33, loaded.player_colours[0].toInt());
        assertEquals(Color.toLinear(loaded.player_colours[0].r()), loaded.linear_player_colours[0].r(), 0.001f);
    }

    @Test
    void testSetPlayerColourUpdatesLinearColours() {
        AccessibilitySettings settings = new AccessibilitySettings();
        Color.Linear[] initialLinear = settings.linear_player_colours;
        Color.Standard customColor = new Color.Standard(0xFF_44_55_66);
        settings.setPlayerColour(1, customColor);

        assertEquals(customColor.toInt(), settings.player_colours[1].toInt());
        assertEquals(Color.toLinear(customColor.r()), settings.linear_player_colours[1].r(), 0.001f);
        // Verify array reference remains synchronized and updated in-place
        assertEquals(initialLinear, settings.linear_player_colours);
    }
}
