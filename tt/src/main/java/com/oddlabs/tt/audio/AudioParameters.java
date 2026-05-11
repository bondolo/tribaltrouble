package com.oddlabs.tt.audio;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.global.Settings;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Parameters used to configure the creation and playback of audio.
 */
public record AudioParameters<S>(
        @NonNull S sound,
        int rank,
        float distance,
        float gain,
        float radius,
        float pitch,
        boolean looping,
        boolean relative,
        boolean music
) {
    public AudioParameters {
        Objects.requireNonNull(sound, "sound");
    }

    public AudioParameters(@NonNull S music_path) {
        this(music_path, AudioPlayer.AUDIO_RANK_MUSIC, AudioPlayer.AUDIO_DISTANCE_MUSIC, Renderer.getRenderer().getSettings().music_gain, 1f, 1f, true, true, true);
    }

    public AudioParameters(@NonNull S sound, int rank, float distance) {
        this(sound, rank, distance, 1f, .5f);
    }

    public AudioParameters(@NonNull S sound, int rank, float distance, float gain, float radius) {
        this(sound, rank, distance, gain, radius, 1f);
    }

    public AudioParameters(@NonNull S sound, int rank, float distance, float gain, float radius, float pitch) {
        this(sound, rank, distance, gain, radius, pitch, false, false);
    }

    public AudioParameters(@NonNull S sound, int rank, float distance, float gain, float radius, float pitch, boolean looping, boolean relative) {
        this(sound, rank, distance, gain, radius, pitch, looping, relative, false);
    }
}
