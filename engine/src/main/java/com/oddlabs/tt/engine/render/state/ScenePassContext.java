package com.oddlabs.tt.engine.render.state;

import com.oddlabs.tt.engine.render.CameraState;
import org.joml.Matrix4fc;

/**
 * Pervasive frame-level state bundle supplied to sub-scene render passes.
 *
 * @param renderContext OpenGL state management context
 * @param camera camera and frustum state
 * @param modelView camera view transformation matrix
 * @param time current frame animation time in seconds
 */
public record ScenePassContext(
                               RenderContext renderContext,
                               CameraState camera,
                               Matrix4fc modelView,
                               float time) {

    /**
     * Returns the camera projection matrix.
     */
    public Matrix4fc projection() {
        return camera.getProjectionMatrix();
    }

    /**
     * Indicates whether the camera is configured in no-detail / low-altitude mode.
     */
    public boolean inNoDetailMode() {
        return camera.inNoDetailMode();
    }
}
