package com.oddlabs.tt.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;
import org.junit.jupiter.api.Test;

final class GUISettingsTest {

  @Test
  void testDefaultValues() {
    GUISettings settings = new GUISettings();
    assertEquals(0.0f, settings.ui_scale);
    assertEquals(0.5f, settings.tooltip_delay);
  }

  @Test
  void testSerializationRoundTrip() {
    GUISettings original = new GUISettings();
    original.ui_scale = 0.75f;
    original.tooltip_delay = 0.25f;

    Properties props = new Properties();
    original.saveToProperties(props);

    GUISettings loaded = new GUISettings();
    loaded.loadFromProperties(props);

    assertEquals(0.75f, loaded.ui_scale);
    assertEquals(0.25f, loaded.tooltip_delay);
  }
}
