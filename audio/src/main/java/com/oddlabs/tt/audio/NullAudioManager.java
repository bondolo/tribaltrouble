package com.oddlabs.tt.audio;

import com.oddlabs.tt.base.animation.AnimationManager;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URL;

/**
 * No-op audio manager implementation used as a fallback when no audio backend is present.
 */
public final class NullAudioManager implements AudioManager {
    private final Vector3f listenerPosition = new Vector3f();
    private final AudioSettings audioSettings;

    public NullAudioManager(AudioSettings audioSettings, AnimationManager animationManager) {
        this.audioSettings = audioSettings;
    }

    @Override
    public AudioPlayer newAudio(float x, float y, float z, AudioParameters params) {
        return new NullAudioPlayer(params);
    }

    @Override
    public void update(float dt) {
    }

    @Override
    public AudioManager startSources() {
        return this;
    }

    @Override
    public AudioManager stopSources() {
        return this;
    }

    @Override
    public Vector3fc getListenerPosition() {
        return listenerPosition;
    }

    @Override
    public float getMasterGain() {
        return audioSettings.sound_gain;
    }

    @Override
    public float getMusicGain() {
        return audioSettings.music_gain;
    }

    @Override
    public float getSfxGain() {
        return audioSettings.sound_gain;
    }

    @Override
    public boolean isEFXSupported() {
        return false;
    }

    @Override
    public boolean isHRTFSupported() {
        return false;
    }

    @Override
    public boolean isSfxEnabled() {
        return audioSettings.play_sfx;
    }

    @Override
    public AudioManager setHeadphoneMode(boolean enabled) {
        return this;
    }

    @Override
    public AudioManager setListenerOrientation(Vector3fc forward, Vector3fc up) {
        return this;
    }

    @Override
    public AudioManager setListenerPosition(float x, float y, float z) {
        listenerPosition.set(x, y, z);
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
    public boolean startPlaying() {
        return false;
    }

    @Override
    public Audio createAudio(URL file) throws IOException {
        return new NullAudio();
    }

    @Override
    public void close() {
    }

    private static final class NullAudio implements Audio {
    }

    private static final class NullAudioPlayer implements AudioPlayer {
        private final AudioParameters params;

        NullAudioPlayer(AudioParameters params) {
            this.params = params;
        }

        @Override
        public AudioParameters getParameters() {
            return params;
        }

        @Override
        public @Nullable AudioSource getSource() {
            return null;
        }

        @Override
        public boolean isPlaying() {
            return false;
        }

        @Override
        public void setGain(float gain) {
        }

        @Override
        public void setPosition(float x, float y, float z) {
        }

        @Override
        public AudioPlayer stop() {
            return this;
        }

        @Override
        public AudioPlayer stop(float decayRate) {
            return this;
        }
    }
}
