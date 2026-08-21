package com.oddlabs.tt.audio;

import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URL;

/**
 * Manages audio playback, including positional audio sources, music, and ambient sounds.
 * Responsible for initializing the audio backend, allocating sources, and controlling global audio properties like
 * listener orientation and master gain.
 */
public interface AudioManager extends AudioImplementation {

    Vector3fc getListenerPosition();

    float getMasterGain();

    float getMusicGain();

    float getSfxGain();

    boolean isEFXSupported();

    boolean isHRTFSupported();

    boolean isSfxEnabled();

    AudioManager setHeadphoneMode(boolean enabled);

    AudioManager setListenerOrientation(Vector3fc forward, Vector3fc up);

    AudioManager setListenerPosition(float x, float y, float z);

    AudioManager setMasterGain(float gain);

    AudioManager setMusicGain(float gain);

    AudioManager setSfxGain(float gain);

    AudioManager setSfxEnabled(boolean enabled);

    boolean startPlaying();

    Audio createAudio(URL file) throws IOException;

    @Override
    AudioPlayer newAudio(float x, float y, float z, AudioParameters params);

    /**
     * Toggles music playback on or off based on current settings.
     */
    default void toggleMusic() {
    }

    /**
     * Enables or disables music playback.
     */
    default void setMusicEnabled(boolean enabled) {
    }

    /**
     * Returns true if music playback is currently enabled in settings.
     */
    default boolean isMusicEnabled() {
        return false;
    }

    /**
     * Sets the background music track with an optional start delay in seconds.
     */
    default void setMusic(AudioParameters musicAudio, float delay) {
    }

    /**
     * Sets the background music track immediately.
     */
    default void setMusic(AudioParameters musicAudio) {
        setMusic(musicAudio, 0f);
    }

    /**
     * Returns the currently active music audio player, or null if no track is playing.
     */
    default @Nullable AudioPlayer getMusicPlayer() {
        return null;
    }

    /**
     * Stops the current music track with the given fade-out rate.
     */
    default void stopMusic(float decayRate) {
    }

    /**
     * Stops the current music track with default fade-out.
     */
    default void stopMusic() {
        stopMusic(1.2f);
    }

    /**
     * Sets the active environmental reverb preset.
     *
     * @param type The reverb type to apply.
     */
    default void setReverb(ReverbType type) {
    }

    /**
     * Blends between two environmental reverb presets.
     *
     * @param from The source reverb environment.
     * @param to The target reverb environment.
     * @param factor Blending factor [0.0 - 1.0].
     */
    default void setReverb(ReverbType from, ReverbType to, float factor) {
    }
}
