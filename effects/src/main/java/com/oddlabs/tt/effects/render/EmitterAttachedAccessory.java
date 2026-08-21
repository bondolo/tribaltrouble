package com.oddlabs.tt.effects.render;

import com.oddlabs.tt.effects.particle.Emitter;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Selectable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * An {@link EmitterAccessory} that wraps a particle {@link Emitter}.
 * Allows particle effects to be attached to units or buildings without quadtree overhead.
 */
public final class EmitterAttachedAccessory implements EmitterAccessory {
    private final Emitter<?> emitter;
    private final Vector3f relativeOffset;

    public EmitterAttachedAccessory(Emitter<?> emitter, Vector3f offset) {
        this.emitter = emitter;
        this.relativeOffset = offset;
    }

    @Override
    public void animate(float t) {
        emitter.animate(t);
    }

    @Override
    public boolean isVisible(Model parent, CameraState camera) {
        if (parent instanceof Selectable<?> selectable) {
            return !selectable.isDead();
        }
        return true;
    }

    @Override
    public void getRelativeTransform(Matrix4f dest, Model parent) {
        dest.translate(relativeOffset);
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return null; // Emitters are handled separately by the emitter_queue
    }

    @Override
    public Emitter<?> getEmitter() {
        return emitter;
    }
}
