package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.gui.ToolTip;
import org.jspecify.annotations.Nullable;

/**
 * Provides picking and tooltips from the 3D scene for GUI interaction.
 */
public interface SceneHoverProvider {
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
}
