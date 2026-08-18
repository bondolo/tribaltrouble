package com.oddlabs.tt.audio;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioFileTest {

    private static final class MockAudioManager implements AudioManager {
        private final Audio dummyAudio = new Audio() {
        };

        @Override
        public @NonNull Audio createAudio(@NonNull URL file) {
            return dummyAudio;
        }

        @Override
        public @NonNull AudioPlayer newAudio(float x, float y, float z, @NonNull AudioParameters params) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull AudioManager setListenerOrientation(@NonNull Vector3fc forward, @NonNull Vector3fc up) {
            return this;
        }

        @Override
        public @NonNull AudioManager setListenerPosition(float x, float y, float z) {
            return this;
        }

        @Override
        public @NonNull AudioManager setHeadphoneMode(boolean enabled) {
            return this;
        }

        @Override
        public @NonNull AudioManager setMasterGain(float gain) {
            return this;
        }

        @Override
        public @NonNull AudioManager setMusicGain(float gain) {
            return this;
        }

        @Override
        public @NonNull AudioManager setSfxGain(float gain) {
            return this;
        }

        @Override
        public @NonNull AudioManager setSfxEnabled(boolean enabled) {
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
        public @NonNull Vector3fc getListenerPosition() {
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
    void testGetThrowsWhenAudioManagerNotBound() {
        AudioFile file = new AudioFile(URI.create("file:///sfx/test.ogg"));
        assertThrows(IllegalStateException.class, file::get);
    }

    @Test
    void testGetResolvesAudioWhenBound() {
        AudioFile file = new AudioFile(URI.create("file:///sfx/test.ogg"));
        MockAudioManager mockManager = new MockAudioManager();

        ScopedValue.where(AudioManager.CURRENT, mockManager).run(() -> {
            Audio audio1 = file.get();
            Audio audio2 = file.get();
            assertNotNull(audio1);
            assertSame(audio1, audio2);
            assertSame(mockManager.dummyAudio, audio1);
        });
    }
}
