package com.oddlabs.tt.audio;

import org.jspecify.annotations.NonNull;

import java.util.function.Function;

/** Returns an audio player for the provided audio parameters. */
@FunctionalInterface
public interface AudioImplementation extends Function<AudioParameters<?>, AbstractAudioPlayer<?>> {
    @NonNull AbstractAudioPlayer<?> newAudio(@NonNull AudioParameters<?> params);

    @Override
    default @NonNull AbstractAudioPlayer<?> apply(@NonNull AudioParameters<?> params) {
        return newAudio(params);
    }
}
