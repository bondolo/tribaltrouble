package com.oddlabs.tt.gui.delegate;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.GUIRenderer;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Interface representing a top-level input and state delegate handled by {@code GUIRoot}.
 */
public interface InputDelegate {
    /**
     * Retrieves the 3D camera state associated with this delegate, if any.
     *
     * @return the camera state, or {@code null} if this delegate has no 3D camera
     */
    default @Nullable CameraState getCameraState() {
        return null;
    }

    /**
     * Renders 2D delegate visual elements using the provided GUI renderer.
     *
     * @param renderer the GUI renderer
     */
    default void render2D(@NonNull GUIRenderer renderer) {
    }

    /**
     * Determines whether this delegate should force rendering even when not on top of the delegate stack.
     *
     * @return true if forced rendering is enabled
     */
    default boolean forceRender() {
        return false;
    }

    /**
     * Determines whether keyboard input is blocked for underlying layers.
     *
     * @return true if keyboard input is blocked
     */
    default boolean keyboardBlocked() {
        return false;
    }

    /**
     * Determines whether the mouse cursor should be rendered when this delegate is active.
     *
     * @return true to render the cursor
     */
    default boolean renderCursor() {
        return true;
    }

    /**
     * Applies projection transformations associated with this delegate's viewport or camera.
     *
     * @param matrix the projection matrix to transform
     * @param width the viewport width in pixels
     * @param height the viewport height in pixels
     * @return the transformed matrix
     */
    default @NonNull Matrix4f multProjection(@NonNull Matrix4f matrix, int width, int height) {
        return matrix;
    }

    /**
     * Determines whether the view can scroll with edge mouse movement.
     *
     * @return true if edge scrolling is enabled
     */
    default boolean canScroll() {
        return false;
    }
}
