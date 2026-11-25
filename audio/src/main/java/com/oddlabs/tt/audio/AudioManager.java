package com.oddlabs.tt.audio;

import java.io.IOException;
import java.net.URL;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/// Manages audio playback, including positional audio sources, music, and ambient sounds. Responsible for initializing
/// the audio backend, allocating sources, and controlling global audio properties like listener orientation and master
/// gain.
public interface AudioManager extends AudioImplementation, AutoCloseable {

  @Override
  default void close() {}

  @Override
  AudioPlayer newAudio(float x, float y, float z, AudioParameters params);

  void update(float dt);

  AudioManager startSources();

  AudioManager stopSources();

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

  default void toggleMusic() {}

  default void setMusicEnabled(boolean enabled) {}

  default boolean isMusicEnabled() {
    return false;
  }

  /// Sets the background music track after waiting `delay` seconds.
  default void setMusic(AudioParameters musicAudio, float delay) {}

  default void setMusic(AudioParameters musicAudio) {
    setMusic(musicAudio, 0f);
  }

  default @Nullable AudioPlayer getMusicPlayer() {
    return null;
  }

  /// Default exponential decay rate for music fade-out (~2.5 seconds).
  float DEFAULT_MUSIC_DECAY_RATE = 1.2f;

  default void stopMusic() {}

  /// Stops the current music track by fading out at the given exponential decay rate.
  default void stopMusic(float decayRate) {}

  default void setReverb(ReverbType type) {}

  /// Blends between two environmental reverb presets (`factor` in `[0.0, 1.0]`).
  default void setReverb(ReverbType from, ReverbType to, float factor) {}
}
