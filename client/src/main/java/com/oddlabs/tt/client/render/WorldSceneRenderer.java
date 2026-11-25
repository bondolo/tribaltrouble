package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.gui.GUIRoot;

/// Interface for rendering the 3D world scene underneath the GUI.
public interface WorldSceneRenderer extends AutoCloseable {
  @Override
  default void close() {}

  void render(RenderContext context, CameraState camera_state, GUIRoot gui_root);

  boolean isClosed();

  default int getVisiblePatchCount() {
    return 0;
  }

  default int getRenderedTreeCount() {
    return 0;
  }

  default long getLandscapePrepNanos() {
    return 0L;
  }

  default long getTreePrepNanos() {
    return 0L;
  }
}
