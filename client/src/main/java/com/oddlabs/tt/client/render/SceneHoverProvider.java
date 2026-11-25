package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.gui.ToolTip;
import org.jspecify.annotations.Nullable;

/// Provides picking and tooltips from the 3D scene for GUI interaction.
public interface SceneHoverProvider {
  /// Performs mouse hover picking against the 3D scene at `(x, y)`.
  ///
  /// @param can_hover_behind whether hovering is allowed behind the active GUI object
  void pickHover(boolean can_hover_behind, CameraState camera, int x, int y);

  @Nullable ToolTip getToolTip();
}
