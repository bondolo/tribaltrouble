package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.simulation.model.Model;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * An accessory that is logically attached to a {@link Model}.
 * These accessories do not exist independently in the world quadtree and instead
 * share the lifecycle and visibility context of their parent.
 */
public sealed interface Accessory extends AutoCloseable permits StaticAccessory, AnimatedAccessory {
    /**
     * {@return The sprite to render, or null if this accessory is rendered via other means (e.g. emitters).}
     */
    @Nullable
    default SpriteKey getSpriteRenderer() {
        return null;
    }

    boolean isVisible(Model parent, CameraState camera);

    default boolean isExpired() {
        return false;
    }

    /** {@return animation index.} */
    default int getAnimation() {
        return 0;
    }

    default float getAnimationTicks(Model parent) {
        return parent.getAnimationTicks();
    }

    default void getRelativeTransform(Matrix4f dest, Model parent) {
    }

    default void getRelativeTransform(Matrix4f dest, ModelState<?> parentState) {
        Model parent = parentState.getModel();
        if (parent != null) {
            getRelativeTransform(dest, parent);
        }
    }

    @Override
    default void close() {
    }
}
