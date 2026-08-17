package com.oddlabs.tt.window;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializableDisplayModeTest {

    @Test
    void testDimensionsAndProperties() {
        SerializableDisplayMode mode = new SerializableDisplayMode(1920, 1080, 32, 60);
        assertEquals(1920, mode.getWidth());
        assertEquals(1080, mode.getHeight());
        assertEquals(32, mode.getBitsPerPixel());
        assertEquals(60, mode.getFrequency());
        assertTrue(SerializableDisplayMode.isModeValid(mode));
        assertNotNull(mode.toString());
    }

    @Test
    void testValidity() {
        SerializableDisplayMode valid = new SerializableDisplayMode(1024, 768, 32, 60);
        assertTrue(SerializableDisplayMode.isModeValid(valid));

        SerializableDisplayMode invalid = new SerializableDisplayMode(320, 240, 32, 60);
        assertFalse(SerializableDisplayMode.isModeValid(invalid));
    }

    @Test
    void testEquivalenceAndComparison() {
        SerializableDisplayMode mode1 = new SerializableDisplayMode(1280, 720, 32, 60);
        SerializableDisplayMode mode2 = new SerializableDisplayMode(1920, 1080, 32, 60);
        SerializableDisplayMode mode3 = new SerializableDisplayMode(1280, 720, 32, 60);
        SerializableDisplayMode mode4 = new SerializableDisplayMode(1280, 720, 16, 60);

        assertTrue(mode1.isEquivalent(mode3));
        assertTrue(mode1.isEquivalent(mode4));
        assertEquals(mode1, mode3);
        assertEquals(mode1.hashCode(), mode3.hashCode());
        assertNotNull(mode1.compareTo(mode2));
    }
}
