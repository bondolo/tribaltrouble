package com.oddlabs.tt.effects.render;

import com.oddlabs.tt.effects.particle.Emitter;
import com.oddlabs.tt.engine.render.AnimatedAccessory;
import java.util.Collection;
import org.jspecify.annotations.Nullable;

/// An accessory that hosts one or more particle [Emitter]s.
public interface EmitterAccessory extends AnimatedAccessory {
  /// Appends any particle emitters managed by this accessory to `dest`.
  default void addEmitters(Collection<Emitter<?>> dest) {
    Emitter<?> emitter = getEmitter();
    if (emitter != null) {
      dest.add(emitter);
    }
  }

  default @Nullable Emitter<?> getEmitter() {
    return null;
  }
}
