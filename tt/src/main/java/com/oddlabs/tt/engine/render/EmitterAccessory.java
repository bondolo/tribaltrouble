package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.effects.render.*;

import com.oddlabs.tt.effects.particle.Emitter;
import org.jspecify.annotations.NonNull;

/**
 * An accessory that hosts a particle {@link Emitter}.
 */
public interface EmitterAccessory extends AnimatedAccessory {
    /** {@return the emitter managed by this accessory} */
    @NonNull
    Emitter<?> getEmitter();
}
