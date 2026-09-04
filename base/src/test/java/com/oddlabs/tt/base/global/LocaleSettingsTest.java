package com.oddlabs.tt.base.global;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LocaleSettingsTest {

    @Test
    void testDefaultValues() {
        LocaleSettings settings = new LocaleSettings();
        assertEquals("default", settings.language);
    }

    @Test
    void testSerializationRoundTrip() {
        LocaleSettings original = new LocaleSettings();
        original.language = "da";

        Properties props = new Properties();
        original.saveToProperties(props);

        LocaleSettings loaded = new LocaleSettings();
        loaded.loadFromProperties(props);

        assertEquals("da", loaded.language);
    }
}
