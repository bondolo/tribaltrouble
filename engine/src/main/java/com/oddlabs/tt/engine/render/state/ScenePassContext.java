package com.oddlabs.tt.engine.render.state;

import com.oddlabs.tt.engine.render.CameraState;
import org.joml.Matrix4fc;

/// Frame-level state bundle supplied to sub-scene render passes.
///
/// @param renderContext OpenGL state management context
/// @param camera camera and frustum state
/// @param modelView camera view transformation matrix
/// @param time current frame animation time in seconds
public record ScenePassContext(RenderContext renderContext, CameraState camera, Matrix4fc modelView, float time) {

  public Matrix4fc projection() {
    return camera.getProjectionMatrix();
  }

  public boolean inNoDetailMode() {
    return camera.inNoDetailMode();
  }
}
