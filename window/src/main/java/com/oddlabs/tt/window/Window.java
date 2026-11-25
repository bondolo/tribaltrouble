package com.oddlabs.tt.window;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

/// Main interface for window management, display mode handling, and fullscreen controls.
public interface Window extends AutoCloseable {
  void create(SerializableDisplayMode mode, boolean fullscreen);

  WindowSettings getSettings();

  void setSettings(WindowSettings settings);

  void setEventListener(@Nullable WindowEventListener listener);

  @Override
  void close();

  void update();

  /// Polls for pending window events without blocking.
  void pollEvents();

  /// Waits indefinitely for at least one window event.
  default void waitEvents() {
    waitEvents(0);
  }

  /// Waits for a window event until one arrives or the timeout expires.
  ///
  /// @param timeoutMs maximum time to wait in milliseconds, or 0 to wait indefinitely
  void waitEvents(int timeoutMs);

  boolean isOpen();

  boolean isCloseRequested();

  void setCloseRequested(boolean value);

  boolean isActive();

  boolean isVisible();

  boolean isIconified();

  boolean isMaximized();

  boolean wasResized();

  /// Returns the physical framebuffer width in pixels (used for OpenGL viewports and buffer allocations).
  int getWidth();

  /// Returns the physical framebuffer height in pixels.
  int getHeight();

  /// Returns the logical window width in screen coordinates (may differ from framebuffer width on High-DPI displays).
  int getLogicalWidth();

  /// Returns the logical window height in screen coordinates.
  int getLogicalHeight();

  void setTitle(String title);

  void setVSyncEnabled(boolean enabled);

  void setFullscreen(boolean fullscreen) throws Exception;

  /// Returns available fullscreen display modes sorted from largest to smallest.
  List<SerializableDisplayMode> getFullscreenDisplayModes();

  /// Returns standard windowed resolutions filtered to fit within the current monitor's usable area.
  List<SerializableDisplayMode> getWindowedDisplayModes();

  SerializableDisplayMode getDisplayMode();

  void setDisplayMode(SerializableDisplayMode mode) throws Exception;

  void setIcon(java.nio.file.Path imagePath);

  void restore();

  void minimize();

  void show();

  void focus();

  void makeCurrent() throws Exception;

  boolean isFullscreen();

  /// Returns the physical monitor dimensions `(width, height)` in millimeters.
  Vector2f getMonitorPhysicalSize();

  Vector2f getMonitorContentScale();

  Vector2f getWindowContentScale();

  float getPixelDensity();

  /// Returns true if mode matches a native hardware exclusive fullscreen mode.
  boolean isExclusiveFullscreenMode(SerializableDisplayMode mode);

  boolean isExclusiveFullscreen();

  default float getDisplayDPI() {
    Vector2f physSize = getMonitorPhysicalSize();
    if (physSize.x() > 0 && physSize.y() > 0) {
      double diagMm = Math.hypot(physSize.x(), physSize.y());
      double diagInches = diagMm / 25.4;
      double diagPixels = Math.hypot(getWidth(), getHeight());
      return (float) (diagPixels / diagInches);
    }
    float scale = getMonitorContentScale().x();
    return 96.0f * (scale > 0 ? scale : 1.0f);
  }

  /// Updates OS chrome visibility (taskbar, dock, menu bar) based on whether simulation gameplay is active.
  void updateSystemUI(boolean playing);

  /// Displays a native open-file dialog and invokes callback with the selected path, or null if cancelled.
  ///
  /// @param filterPattern file extension filter (e.g., "json")
  void showOpenFileDialog(String filterName, String filterPattern, Consumer<@Nullable Path> callback);

  /// Displays a native save-file dialog and invokes callback with the selected path, or null if cancelled.
  ///
  /// @param filterPattern file extension filter (e.g., "json")
  /// @param defaultLocation default file name or initial path
  void showSaveFileDialog(
      String filterName, String filterPattern, @Nullable String defaultLocation, Consumer<@Nullable Path> callback);
}
