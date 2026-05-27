package com.oddlabs.tt.model;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.render.SpriteKey;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An accessory that is logically attached to an {@link AccessorizableModel}.
 * These accessories do not exist independently in the world quadtree and instead
 * share the lifecycle and visibility context of their parent.
 */
public sealed interface Accessory permits StaticAccessory, AnimatedAccessory {
    /**
     * Returns the sprite to render, or null if this accessory is rendered via other means (e.g. emitters).
     *
     * @return The sprite renderer.
     */
    @Nullable
    SpriteKey getSpriteRenderer();

    /**
     * Returns true if the accessory should currently be drawn.
     *
     * @param parent The model this accessory is attached to.
     * @param camera The current camera state for distance/visibility checks.
     * @return visibility status.
     */
    boolean isVisible(@NonNull AccessorizableModel parent, @NonNull CameraState camera);

    /**
     * Returns true if this accessory has completed its lifecycle and should be removed.
     *
     * @return True if expired.
     */
    default boolean isExpired() {
        return false;
    }

    /**
     * Returns the animation index to use for this accessory.
     *
     * @return animation index.
     */
    default int getAnimation() {
        return 0;
    }

    /**
     * Returns the animation ticks to use for this accessory.
     *
     * @param parent The model this accessory is attached to.
     * @return animation ticks.
     */
    default float getAnimationTicks(@NonNull AccessorizableModel parent) {
        return parent.getAnimationTicks();
    }

    /**
     * Provides the transform relative to the parent's position.
     *
     * @param dest   The matrix to populate with the relative transform.
     * @param parent The model this accessory is attached to.
     */
    void getRelativeTransform(@NonNull Matrix4f dest, @NonNull AccessorizableModel parent);
}
