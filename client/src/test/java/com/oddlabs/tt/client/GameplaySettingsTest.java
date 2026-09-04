package com.oddlabs.tt.client;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GameplaySettingsTest {

    @Test
    void testDefaultValues() {
        GameplaySettings settings = new GameplaySettings();
        assertFalse(settings.aggressive_units);
    }

    @Test
    void testSerializationRoundTrip() {
        GameplaySettings original = new GameplaySettings();
        original.aggressive_units = true;

        Properties props = new Properties();
        original.saveToProperties(props);

        GameplaySettings loaded = new GameplaySettings();
        loaded.loadFromProperties(props);

        assertTrue(loaded.aggressive_units);
    }
}
