package com.oddlabs.tt.audio;

import com.oddlabs.tt.base.animation.AnimationManager;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/// Service provider interface for creating {@link AudioManager} backends.
public interface AudioProvider {
  Logger logger = Logger.getLogger(AudioProvider.class.getName());

  AudioManager create(AudioSettings settings, AnimationManager animationManager);

  /// Loads the {@link AudioManager} via {@link ServiceLoader}, falling back to OpenAL if none is registered.
  static AudioManager load(AudioSettings settings, AnimationManager animationManager) {
    Optional<AudioProvider> provider =
        ServiceLoader.load(AudioProvider.class, AudioProvider.class.getClassLoader()).findFirst();

    if (provider.isEmpty()) {
      provider = ServiceLoader.load(AudioProvider.class).findFirst();
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
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("No AudioProvider found", e);
    }
  }
}
