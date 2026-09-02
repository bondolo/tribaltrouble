package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioProvider;
import com.oddlabs.tt.audio.AudioSettings;
import com.oddlabs.tt.base.animation.AnimationManager;

/**
 * OpenAL backend service provider for {@link AudioProvider}.
 */
public final class OpenALAudioProvider implements AudioProvider {
    @Override
    public AudioManager create(AudioSettings settings, AnimationManager animationManager) {
        return new OpenALManager(settings, animationManager);
    }
}
