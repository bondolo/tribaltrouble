package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.gui.GUIRoot;

/**
 * Interface for rendering the 3D world scene underneath the GUI.
 */
public interface WorldSceneRenderer extends AutoCloseable {
    /**
     * Closes the renderer and releases any underlying graphics resources.
     */
    @Override
    default void close() {
    }

    /**
     * Renders the 3D scene.
     *
     * @param context the current render context
     * @param camera_state the active camera frustum state
     * @param gui_root the root GUI container
     */
    void render(RenderContext context, CameraState camera_state, GUIRoot gui_root);

    /**
     * Checks if this renderer has been closed.
     *
     * @return true if closed
     */
    boolean isClosed();

    /**
     * Returns the number of landscape patches drawn in the last frame.
     *
     * @return visible patch count
     */
    default int getVisiblePatchCount() {
        return 0;
    }

    /**
     * Returns the number of tree instances rendered in the last frame.
     *
     * @return rendered tree count
     */
    default int getRenderedTreeCount() {
        return 0;
    }

    /**
     * Returns CPU nanoseconds spent preparing landscape geometry for the last frame.
     *
     * @return landscape preparation time in nanoseconds
     */
    default long getLandscapePrepNanos() {
        return 0L;
    }

    /**
     * Returns CPU nanoseconds spent preparing tree geometry for the last frame.
     *
     * @return tree preparation time in nanoseconds
     */
    default long getTreePrepNanos() {
        return 0L;
    }
}
