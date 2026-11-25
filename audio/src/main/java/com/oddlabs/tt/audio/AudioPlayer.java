package com.oddlabs.tt.audio;

import org.jspecify.annotations.Nullable;

/// Manages the playback of a single audio instance associated with an {@link AudioSource}.
public interface AudioPlayer {

  AudioParameters getParameters();

  @Nullable AudioSource getSource();

  boolean isPlaying();

  void setGain(float gain);

  void setPosition(float x, float y, float z);

  AudioPlayer stop();

  /// Stops playback by fading volume out exponentially according to `G(t) = G(0) * e^(-decayRate * t)`, where `t` is
  /// elapsed time in seconds, stopping the source once gain drops below the silence threshold.
  ///
  /// For reference:
  /// - `15.0` produces a fast fade-out (~0.2 seconds).
  /// - `1.2` produces a gradual fade-out (~2.5 seconds).
  ///
  /// @param decayRate exponential decay rate (higher values result in a faster fade-out)
  AudioPlayer stop(float decayRate);
}
