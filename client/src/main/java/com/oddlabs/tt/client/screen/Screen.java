package com.oddlabs.tt.client.screen;

import com.oddlabs.tt.client.render.SceneHoverProvider;
import com.oddlabs.tt.client.render.WorldSceneRenderer;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.gui.GUIRoot;
import org.jspecify.annotations.Nullable;

/// Application screen combining a 2D user interface and an optional 3D scene.
public interface Screen extends AutoCloseable {
  GUIRoot guiRoot();

  @Nullable WorldSceneRenderer sceneRenderer();

  @Nullable SceneHoverProvider hoverProvider();

  /// Renders the 3D scene if present and open, or clears the viewport otherwise.
  default void render3D(RenderContext context, CameraState camera) {
    var renderer = sceneRenderer();
    if (renderer != null && !renderer.isClosed()) {
      renderer.render(context, camera, guiRoot());
    } else {
      context.clear(true, true);
    }
  }

  default Screen withGUIRoot(GUIRoot newRoot) {
    return new DefaultScreen(newRoot, sceneRenderer(), hoverProvider());
  }

  @Override
  default void close() {
    var renderer = sceneRenderer();
    if (renderer != null) {
      renderer.close();
    }
  }
}
