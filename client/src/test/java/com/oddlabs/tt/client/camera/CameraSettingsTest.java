package com.oddlabs.tt.client.camera;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CameraSettingsTest {

    @Test
    void testDefaultValues() {
        CameraSettings settings = new CameraSettings();
        assertFalse(settings.invert_camera_pitch);
        assertEquals(0.5f, settings.mapmode_delay);
    }

    @Test
    void testSerializationRoundTrip() {
        CameraSettings original = new CameraSettings();
        original.invert_camera_pitch = true;
        original.mapmode_delay = 1.0f;

        Properties props = new Properties();
        original.saveToProperties(props);

        CameraSettings loaded = new CameraSettings();
        loaded.loadFromProperties(props);

        assertTrue(loaded.invert_camera_pitch);
        assertEquals(1.0f, loaded.mapmode_delay);
    }
}
