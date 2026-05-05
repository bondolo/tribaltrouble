package com.oddlabs.tt.audio;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Interface for audio backend implementations to create audio players.
 */
@FunctionalInterface
public interface AudioImplementation extends Function<AudioParameters<?>, AudioPlayer> {
    @NonNull AudioPlayer newAudio(@NonNull AudioParameters<?> params);

    @Override
    default @NonNull AudioPlayer apply(@NonNull AudioParameters<?> params) {
        return newAudio(params);
    }
}
