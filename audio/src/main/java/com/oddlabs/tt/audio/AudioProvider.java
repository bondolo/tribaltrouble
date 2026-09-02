package com.oddlabs.tt.audio;

import com.oddlabs.tt.base.animation.AnimationManager;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Service provider interface for creating {@link AudioManager} backends.
 */
public interface AudioProvider {
    Logger logger = Logger.getLogger(AudioProvider.class.getName());

    /**
     * Creates and initializes an {@link AudioManager} instance.
     *
     * @param settings audio configuration settings
     * @param animationManager animation manager for scheduling fade and audio tasks
     * @return an initialized {@link AudioManager} instance
     */
    AudioManager create(AudioSettings settings, AnimationManager animationManager);

    /**
     * Loads the preferred {@link AudioManager} instance via {@link ServiceLoader}.
     * If no audio provider service is found, returns a no-op {@link NullAudioManager}.
     *
     * @param settings audio configuration settings
     * @param animationManager animation manager for scheduling fade and audio tasks
     * @return an initialized {@link AudioManager} instance
     */
    static AudioManager load(AudioSettings settings, AnimationManager animationManager) {
        Optional<AudioProvider> provider = ServiceLoader.load(AudioProvider.class, AudioProvider.class.getClassLoader())
                .findFirst();

        if (provider.isEmpty()) {
            provider = ServiceLoader.load(AudioProvider.class)
                    .findFirst();
        }

        if (provider.isPresent()) {
            AudioProvider p = provider.get();
            logger.info("Loaded AudioProvider: " + p.getClass().getName());
            return p.create(settings, animationManager);
        }

        try {
            Class<?> clazz = Class.forName("com.oddlabs.tt.audio.openal.OpenALAudioProvider");
            AudioProvider p = (AudioProvider) clazz.getDeclaredConstructor().newInstance();
            logger.info("Loaded AudioProvider via fallback: " + p.getClass().getName());
            return p.create(settings, animationManager);
        } catch (Throwable t) {
            logger.info("No AudioProvider found; using NullAudioManager fallback (" + t.getMessage() + ")");
            return new NullAudioManager(settings, animationManager);
        }
    }
}
