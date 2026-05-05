package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * OpenAL implementation of {@link AudioPlayer} for buffered audio.
 */
final class OpenALAudioPlayer extends AudioPlayer {

    public OpenALAudioPlayer(@Nullable OpenALAudioSource source, @NonNull AudioParameters<Audio> params) {
        super(source, params);
        if (this.source == null) {
            return;
        }

        if (params.music || AudioManager.getManager().startPlaying()) {
            source.play();
        }
    }
}
