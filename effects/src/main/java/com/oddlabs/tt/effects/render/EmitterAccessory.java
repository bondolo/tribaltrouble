package com.oddlabs.tt.effects.render;

import com.oddlabs.tt.effects.particle.Emitter;
import com.oddlabs.tt.engine.render.AnimatedAccessory;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * An accessory that hosts one or more particle {@link Emitter}s.
 */
public interface EmitterAccessory extends AnimatedAccessory {
    /**
     * Appends any particle emitters managed by this accessory to the destination collection.
     *
     * @param dest The collection to append emitters to.
     */
    default void addEmitters(Collection<Emitter<?>> dest) {
        Emitter<?> emitter = getEmitter();
        if (emitter != null) {
            dest.add(emitter);
        }
    }

    /**
     * Returns the primary emitter managed by this accessory, or null if managed individually.
     *
     * @return the emitter, or null
     */
    default @Nullable Emitter<?> getEmitter() {
        return null;
    }
}
