package com.oddlabs.tt.engine.settings;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Properties;
import org.junit.jupiter.api.Test;

final class GraphicsSettingsTest {

  @Test
  void testSaveAndLoadProperties() {
    GraphicsSettings settings = new GraphicsSettings();
    Properties props = new Properties();
    assertDoesNotThrow(() -> settings.saveToProperties(props));

    GraphicsSettings loaded = new GraphicsSettings();
    assertDoesNotThrow(() -> loaded.loadFromProperties(props));
  }
}
