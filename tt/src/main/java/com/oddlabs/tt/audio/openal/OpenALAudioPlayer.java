package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * OpenAL implementation of {@link AudioPlayer} for buffered audio.
 */
final class OpenALAudioPlayer extends AudioPlayer {

    public OpenALAudioPlayer(@Nullable OpenALAudioSource source, float x, float y, float z,
            @NonNull AudioParameters params) {
        super(source, x, y, z, params);
        if (this.source == null) {
            return;
        }

        if (params.audio().isStreaming() || Renderer.getRenderer().getAudioManager().startPlaying()) {
            source.play();
        }
    }

    @Override
    protected int getBufferCount() {
        return 1;
    }
}
