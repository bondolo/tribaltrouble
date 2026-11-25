package com.oddlabs.tt.client;

import com.oddlabs.tt.window.SerializableDisplayMode;

/// Controller for toggling fullscreen and switching window/display resolution modes.
public interface DisplayModeController {
  void toggleFullscreen();

  /// Switches display mode immediately, or defers the change until the next restart when `switchNow` is false.
  void switchMode(SerializableDisplayMode mode, boolean switchNow);
}
