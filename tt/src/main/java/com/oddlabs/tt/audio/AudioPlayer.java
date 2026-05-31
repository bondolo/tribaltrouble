package com.oddlabs.tt.audio;

import com.oddlabs.tt.animation.Animated;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Manages the playback of a single audio instance associated with an {@link AudioSource}.
 */
public interface AudioPlayer extends Animated {

    @NonNull
    AudioParameters getParameters();

    @Nullable
    AudioSource getSource();

    boolean isPlaying();

    void setGain(float gain);

    void setPosition(float x, float y, float z);

    @NonNull
    AudioPlayer stop();

    @NonNull
    AudioPlayer stop(float delay, float end_gain);
}
