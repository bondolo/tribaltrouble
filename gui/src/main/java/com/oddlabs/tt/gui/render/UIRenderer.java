package com.oddlabs.tt.gui.render;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.ToolTip;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Interface for rendering the 3D scene underneath the 2D user interface.
 */
public interface UIRenderer {
    /**
     * Renders the 3D scene.
     *
     * @param context the current render context
     * @param camera_state the active camera frustum state
     * @param gui_root the root GUI container
     */
    void render(RenderContext context, CameraState camera_state, GUIRoot gui_root);

    /**
     * Performs mouse hover picking against the 3D scene.
     *
     * @param can_hover_behind whether hovering is allowed behind the active GUI object
     * @param camera the active camera state
     * @param x the mouse X coordinate
     * @param y the mouse Y coordinate
     */
    void pickHover(boolean can_hover_behind, CameraState camera, int x, int y);

    /**
     * Retrieves the tool tip text or descriptor provided by the underlying scene.
     *
     * @return the active tool tip, or null if none
     */
    @Nullable
    ToolTip getToolTip();

    /**
     * Checks if developer cheat mode is enabled for the renderer.
     *
     * @return true if cheat mode is active
     */
    boolean isCheater();

    /**
     * Prepares frame rendering for the 3D scene.
     *
     * @param context the current render context
     */
    void startFrame(RenderContext context);

    /**
     * Finalizes frame rendering and invokes the 2D GUI overlay callback.
     *
     * @param context the current render context
     * @param guiRenderCallback callback to render the 2D GUI overlay
     */
    void endFrame(RenderContext context, Consumer<RenderContext> guiRenderCallback);

    /**
     * Checks if this renderer has been closed.
     *
     * @return true if closed
     */
    boolean isClosed();
}
