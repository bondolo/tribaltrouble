package com.oddlabs.tt.audio;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioSettingsTest {

    @Test
    void testDefaultValues() {
        AudioSettings settings = new AudioSettings();
        assertTrue(settings.play_music);
        assertTrue(settings.play_sfx);
        assertFalse(settings.headphone_mode);
        assertEquals(0.5f, settings.music_gain, 0.001f);
        assertEquals(1.0f, settings.sound_gain, 0.001f);
        assertTrue(settings.warning_no_sound);
    }

    @Test
    void testPropertiesSerialization() {
        AudioSettings original = new AudioSettings();
        original.play_music = false;
        original.play_sfx = false;
        original.headphone_mode = true;
        original.music_gain = 0.8f;
        original.sound_gain = 0.6f;
        original.warning_no_sound = false;

        Properties props = new Properties();
        original.saveToProperties(props);

        AudioSettings loaded = new AudioSettings();
        loaded.loadFromProperties(props);

        assertFalse(loaded.play_music);
        assertFalse(loaded.play_sfx);
        assertTrue(loaded.headphone_mode);
        assertEquals(0.8f, loaded.music_gain, 0.001f);
        assertEquals(0.6f, loaded.sound_gain, 0.001f);
        assertFalse(loaded.warning_no_sound);
    }
}
