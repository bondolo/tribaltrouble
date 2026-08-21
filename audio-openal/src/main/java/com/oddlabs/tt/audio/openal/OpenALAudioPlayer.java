package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import org.jspecify.annotations.Nullable;

/**
 * OpenAL implementation of {@link AudioPlayer} for buffered audio.
 */
final class OpenALAudioPlayer extends AbstractAudioPlayer<OpenALManager, OpenALAudioSource> {

    public OpenALAudioPlayer(OpenALManager manager, @Nullable OpenALAudioSource source,
            float x, float y, float z, AudioParameters params) {
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
