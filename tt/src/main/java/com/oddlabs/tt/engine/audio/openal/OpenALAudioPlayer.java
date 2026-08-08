package com.oddlabs.tt.engine.audio.openal;

import com.oddlabs.tt.engine.audio.AbstractAudioPlayer;
import com.oddlabs.tt.engine.audio.AudioParameters;
import com.oddlabs.tt.engine.audio.AudioPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * OpenAL implementation of {@link AudioPlayer} for buffered audio.
 */
final class OpenALAudioPlayer extends AbstractAudioPlayer<OpenALManager, OpenALAudioSource> {

    public OpenALAudioPlayer(@NonNull OpenALManager manager, @Nullable OpenALAudioSource source,
            float x, float y, float z, @NonNull AudioParameters params) {
        super(manager, source, x, y, z, params);
        if (this.source == null) {
            return;
        }

        if (params.audio().isStreaming() || manager.startPlaying()) {
            source.play();
        }
    }

    @Override
    protected int getBufferCount() {
        return 1;
    }
}
