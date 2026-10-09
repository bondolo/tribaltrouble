package com.oddlabs.tt.gui.render;

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
}
