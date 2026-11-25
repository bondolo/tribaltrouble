package com.oddlabs.tt.gui.delegate;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.GUIRenderer;
import org.jspecify.annotations.Nullable;

/// Top-level input and state delegate handled by `GUIRoot`.
public interface InputDelegate {
  default @Nullable CameraState getCameraState() {
    return null;
  }

  default void updateView(int width, int height) {}

  default void render2D(GUIRenderer renderer) {}

  /// Whether this delegate renders even when not on top of the delegate stack.
  default boolean forceRender() {
    return false;
  }

  /// Whether keyboard input is blocked for underlying delegates.
  default boolean keyboardBlocked() {
    return false;
  }

  default boolean renderCursor() {
    return true;
  }

  /// Whether the viewport can scroll via edge mouse movement.
  default boolean canScroll() {
    return false;
  }
}
