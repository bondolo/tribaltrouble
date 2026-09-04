package com.oddlabs.tt.engine.settings;

import com.oddlabs.tt.base.global.AppConfig;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GraphicsSettingsTest {

    @Test
    void testDefaultValues() {
        GraphicsSettings settings = new GraphicsSettings();
        assertEquals(AppConfig.DEFAULT_DETAIL_NORMAL, settings.graphic_detail);
    }

    @Test
    void testSaveAndLoadProperties() {
        GraphicsSettings settings = new GraphicsSettings();
        settings.graphic_detail = 2;

        Properties props = new Properties();
        settings.saveToProperties(props);

        GraphicsSettings loaded = new GraphicsSettings();
        loaded.loadFromProperties(props);

        assertEquals(2, loaded.graphic_detail);
    }
}
