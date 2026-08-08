package com.oddlabs.tt.render;

import com.oddlabs.tt.client.camera.CameraState;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.effects.particle.Emitter;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An {@link AnimatedAccessory} that wraps a particle {@link Emitter}.
 * Allows particle effects to be attached to units or buildings without quadtree overhead.
 */
public final class EmitterAttachedAccessory implements EmitterAccessory {
    private final @NonNull Emitter<?> emitter;
    private final @NonNull Vector3f relativeOffset;

    public EmitterAttachedAccessory(@NonNull Emitter<?> emitter, @NonNull Vector3f offset) {
        this.emitter = emitter;
        this.relativeOffset = offset;
    }

    @Override
    public void animate(float t) {
        emitter.animate(t);
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        if (parent instanceof Selectable<?> selectable) {
            return !selectable.isDead();
        }
        return true;
    }

    @Override
    public void getRelativeTransform(@NonNull Matrix4f dest, @NonNull Model parent) {
        // Apply rotation based on parent direction
        float angle = (float) Math.atan2(parent.getDirectionY(), parent.getDirectionX());
        dest.rotate(angle, 0f, 0f, 1f);
        dest.translate(relativeOffset);
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return null; // Emitters are handled separately by the emitter_queue
    }

    @Override
    public @NonNull Emitter<?> getEmitter() {
        return emitter;
    }
}
