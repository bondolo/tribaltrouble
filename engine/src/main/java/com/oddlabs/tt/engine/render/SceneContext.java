package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.simulation.model.Model;
import org.jspecify.annotations.Nullable;

/// Context interface providing element render states with access to render queues, active camera state, and model
/// response status without coupling to client state.
public interface SceneContext {
  RenderQueues getRenderQueues();

  @Nullable CameraState getCamera();

  /// Returns whether `model` is currently in a response state (e.g., selection animation).
  boolean isResponding(Model model);

  /// Returns the sub-tick presentation interpolation factor in `[0, 1]`.
  default float getInterpolationAlpha() {
    return 1.0f;
  }
}
