package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioProvider;
import com.oddlabs.tt.audio.AudioSettings;
import com.oddlabs.tt.base.animation.AnimationManager;

/**
 * OpenAL backend service provider for {@link AudioProvider}.
 */
public final class OpenALAudioProvider implements AudioProvider {

    /** System property that, when set to "true", wraps the manager in a silent (zero-master-gain) device. */
    private static final String SILENT_PROPERTY = "com.oddlabs.tt.audio.silent";
    /**
     * Environment-variable equivalent of {@link #SILENT_PROPERTY}, used when the flag is not set as a system property.
     */
    private static final String SILENT_ENV = "TT_AUDIO_SILENT";

    @Override
    public AudioManager create(AudioSettings settings, AnimationManager animationManager) {
        if (isSilentRequested()) {
            return new SilentAudioManager(settings, animationManager);
        }
        return new OpenALManager(settings, animationManager);
    }

    /** True when the silent device is requested via the system property or its environment-variable equivalent. */
    private static boolean isSilentRequested() {
        return "true".equals(System.getProperty(SILENT_PROPERTY)) || "true".equals(System.getenv(SILENT_ENV));
    }
}
