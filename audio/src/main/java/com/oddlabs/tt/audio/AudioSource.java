package com.oddlabs.tt.audio;

import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/// A point in 3D space that emits sound, abstracting the underlying audio implementation.
public interface AudioSource {

  /// Playback lifecycle state of an audio source.
  enum State {
    INITIAL,
    PLAYING,
    PAUSED,
    STOPPED
  }

  State getState();

  void setAudio(Audio audio);

  void setPitch(float pitch);

  void setGain(float gain);

  void setMinGain(float gain);

  void setMaxGain(float gain);

  void setRolloff(float rolloff);

  void setDistance(float distance);

  float getRolloff();

  float getDistance();

  void setPosition(float x, float y, float z);

  void setRelative(boolean relative);

  void setLooping(boolean looping);

  void stop();

  void pause();

  void play();

  void setBuffer(int bufferId);

  void rewind();

  Vector3f getPosition();

  int getRank();

  @Nullable AudioPlayer getAudioPlayer();

  void setAudioPlayer(@Nullable AudioPlayer audioPlayer);

  /// Routes this source to an auxiliary effect slot (e.g., for reverb).
  ///
  /// @param slotId effect slot ID
  /// @param filterId filter ID to apply to the send, or `0` for none
  void setAuxiliarySend(int slotId, int filterId);

  /// Sets the direct-path high-frequency filter gain (air absorption) in `[0.0, 1.0]`.
  void setDirectFilterGainHF(float gainHF);
}
