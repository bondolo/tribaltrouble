package com.oddlabs.tt.model;

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
     * @return visibility status.
     */
    boolean isVisible(@NonNull AccessorizableModel parent);

    /**
     * Provides the transform relative to the parent's position.
     *
     * @param dest   The matrix to populate with the relative transform.
     * @param parent The model this accessory is attached to.
     */
    void getRelativeTransform(@NonNull Matrix4f dest, @NonNull AccessorizableModel parent);
}
