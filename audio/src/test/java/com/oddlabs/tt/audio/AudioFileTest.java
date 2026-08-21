package com.oddlabs.tt.audio;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioFileTest {

    private static final class MockAudioManager implements AudioManager {
        private final Audio dummyAudio = new Audio() {
        };

        @Override
        public Audio createAudio(URL file) {
            return dummyAudio;
        }

        @Override
        public AudioPlayer newAudio(float x, float y, float z, AudioParameters params) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AudioManager setListenerOrientation(Vector3fc forward, Vector3fc up) {
            return this;
        }

        @Override
        public AudioManager setListenerPosition(float x, float y, float z) {
            return this;
        }

        @Override
        public AudioManager setHeadphoneMode(boolean enabled) {
            return this;
        }

        @Override
        public AudioManager setMasterGain(float gain) {
            return this;
        }

        @Override
        public AudioManager setMusicGain(float gain) {
            return this;
        }

        @Override
        public AudioManager setSfxGain(float gain) {
            return this;
        }

        @Override
        public AudioManager setSfxEnabled(boolean enabled) {
            return this;
        }

        @Override
        public float getMasterGain() {
            return 1f;
        }

        @Override
        public float getMusicGain() {
            return 1f;
        }

        @Override
        public float getSfxGain() {
            return 1f;
        }

        @Override
        public boolean isSfxEnabled() {
            return true;
        }

        @Override
        public boolean isHRTFSupported() {
            return false;
        }

        @Override
        public boolean isEFXSupported() {
            return false;
        }

        @Override
        public boolean startPlaying() {
            return true;
        }

        @Override
        public Vector3fc getListenerPosition() {
            return new Vector3f();
        }
    }

    @Test
    void testAudioFileStreamingDetection() {
        AudioFile musicFile = new AudioFile(URI.create("file:///music/theme.ogg"));
        AudioFile sfxFile = new AudioFile(URI.create("file:///sfx/hit.ogg"));
        AudioFile customStreamFile = new AudioFile(URI.create("file:///sfx/long_stream.ogg"), true);

        assertTrue(musicFile.isStreaming());
        assertFalse(sfxFile.isStreaming());
        assertTrue(customStreamFile.isStreaming());
    }

    @Test
    void testAudioFileEquality() {
        AudioFile file1 = new AudioFile(URI.create("file:///sfx/hit.ogg"));
        AudioFile file2 = new AudioFile(URI.create("file:///sfx/hit.ogg"));
        AudioFile file3 = new AudioFile(URI.create("file:///sfx/other.ogg"));

        assertEquals(file1, file2);
        assertEquals(file1.hashCode(), file2.hashCode());
        assertNotEquals(file1, file3);
    }

    @Test
    void testGetResolvesAudioWithManager() {
        AudioFile file = new AudioFile(URI.create("file:///sfx/test.ogg"));
        MockAudioManager mockManager = new MockAudioManager();

        Audio audio1 = file.get(mockManager);
        Audio audio2 = file.get(mockManager);
        assertNotNull(audio1);
        assertSame(audio1, audio2);
        assertSame(mockManager.dummyAudio, audio1);
    }
}
