package com.oddlabs.tt.audio;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Parameters used to configure the creation and playback of audio.
 *
 * @param audio    The audio file to play.
 * @param rank     Priority of the audio. Higher rank means it's less likely to be preempted when channels are
 *                 exhausted.
 * @param distance The maximum distance at which the sound can be heard. Sounds beyond this distance are culled or
 *                 muted.
 * @param gain     The base volume multiplier (0.0 to 1.0+). Higher values increase the base volume.
 * @param radius   The reference distance for attenuation. Sounds closer than this radius play at their maximum gain.
 *                 Increasing this makes the sound seem larger or louder over a greater area.
 * @param pitch    The playback speed/pitch multiplier (1.0 is normal).
 * @param looping  True if the audio should loop continuously.
 * @param relative True if coordinates are relative to the listener, false for absolute world coordinates.
 */
public record AudioParameters(
                              @NonNull AudioFile audio,
                              int rank,
                              float distance,
                              float gain,
                              float radius,
                              float pitch,
                              boolean looping,
                              boolean relative
) {
    public AudioParameters {
        Objects.requireNonNull(audio, "audio");
    }

    public AudioParameters(@NonNull AudioFile audio, int rank, float distance) {
        this(audio, rank, distance, 1f, .5f);
    }

    public AudioParameters(@NonNull AudioFile audio, int rank, float distance, float gain, float radius) {
        this(audio, rank, distance, gain, radius, 1f);
    }

    public AudioParameters(@NonNull AudioFile audio, int rank, float distance, float gain, float radius, float pitch) {
        this(audio, rank, distance, gain, radius, pitch, false, false);
    }
}
