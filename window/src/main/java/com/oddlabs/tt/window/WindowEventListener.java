package com.oddlabs.tt.window;

import org.lwjgl.sdl.SDL_Event;

/// Listener interface for receiving raw windowing and input events from the window.
public interface WindowEventListener {
  void handleSDLEvent(SDL_Event event);

  default void onFocusGained() {}

  /// Called when the framebuffer size changes, with new dimensions in pixels.
  default void onResized(int width, int height) {}

  default void onToggleFullscreen() {}
}
