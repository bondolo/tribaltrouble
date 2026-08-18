package com.oddlabs.tt.audio;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioParametersTest {

    @Test
    void testAudioParametersCreationAndDefaults() {
        AudioFile file = new AudioFile(URI.create("file:///test.ogg"), false);
        AudioParameters params = new AudioParameters(file, 10, 50.0f);

        assertEquals(10, params.rank());
        assertEquals(50.0f, params.distance(), 0.001f);
        assertEquals(1.0f, params.gain(), 0.001f);
        assertEquals(0.5f, params.radius(), 0.001f);
        assertEquals(1.0f, params.pitch(), 0.001f);
        assertFalse(params.looping());
        assertFalse(params.relative());
        assertFalse(params.ambient());
        assertFalse(params.audio().isStreaming());
    }

    @Test
    void testAudioParametersCustomValues() {
        AudioFile file = new AudioFile(URI.create("file:///music/test.ogg"), true);
        AudioParameters params = new AudioParameters(file, 20, 100.0f, 0.8f, 10.0f, 1.2f, true, true);

        assertEquals(20, params.rank());
        assertEquals(100.0f, params.distance(), 0.001f);
        assertEquals(0.8f, params.gain(), 0.001f);
        assertEquals(10.0f, params.radius(), 0.001f);
        assertEquals(1.2f, params.pitch(), 0.001f);
        assertTrue(params.looping());
        assertTrue(params.relative());
        assertFalse(params.ambient());
        assertTrue(params.audio().isStreaming());
    }

    @Test
    void testNullAudioThrows() {
        assertThrows(NullPointerException.class, () -> new AudioParameters(null, 10, 50.0f));
    }
}
