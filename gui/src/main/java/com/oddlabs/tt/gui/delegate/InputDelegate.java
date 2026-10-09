package com.oddlabs.tt.gui.delegate;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.GUIRenderer;
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
     * Updates the camera view and projection matrices for this delegate.
     *
     * @param width the viewport width in pixels
     * @param height the viewport height in pixels
     */
    default void updateView(int width, int height) {
    }

    /**
     * Renders 2D delegate visual elements using the provided GUI renderer.
     *
     * @param renderer the GUI renderer
     */
    default void render2D(GUIRenderer renderer) {
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
     * Determines whether the view can scroll with edge mouse movement.
     *
     * @return true if edge scrolling is enabled
     */
    default boolean canScroll() {
        return false;
    }
}
