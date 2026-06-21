package com.oddlabs.tt.window;

import com.oddlabs.tt.render.SerializableDisplayMode;
import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Main interface for window management, display mode handling, and fullscreen controls.
 */
public interface Window extends AutoCloseable {
    void create(@NonNull SerializableDisplayMode mode, boolean fullscreen);

    @Override
    void close();

    void update();

    void pollEvents();

    boolean isOpen();

    boolean isCloseRequested();

    void setCloseRequested(boolean value);

    boolean isActive();

    boolean isVisible();

    boolean isIconified();

    boolean isMaximized();

    boolean wasResized();

    /**
     * Returns the physical framebuffer width in pixels. This is the size used for
     * OpenGL viewport and buffer allocations.
     *
     * @return int width in pixels
     */
    int getWidth();

    /**
     * Returns the physical framebuffer height in pixels.
     *
     * @return int height in pixels
     */
    int getHeight();

    /**
     * Returns the logical window width in screen coordinates.
     * Screen coordinates are used for window positioning and cursor input.
     * On high-DPI displays (e.g. Retina), this may be smaller than the framebuffer width.
     *
     * @return int width in screen coordinates
     */
    int getLogicalWidth();

    /**
     * Returns the logical window height in screen coordinates.
     *
     * @return int height in screen coordinates
     */
    int getLogicalHeight();

    void setTitle(String title);

    void setVSyncEnabled(boolean enabled);

    void setFullscreen(boolean fullscreen) throws Exception;

    /**
     * Returns a list of resolutions available for fullscreen mode sorted from largest to smallest.
     *
     * @return List of SerializableDisplayMode
     */
    @NonNull
    List<@NonNull SerializableDisplayMode> getFullscreenDisplayModes();

    /**
     * Returns a list of standard resolutions suitable for windowed mode, filtered to fit within the usable area of the
     * current monitor.
     *
     * @return List of SerializableDisplayMode
     */
    @NonNull
    List<@NonNull SerializableDisplayMode> getWindowedDisplayModes();

    /** {@return the current display mode} */
    @NonNull
    SerializableDisplayMode getDisplayMode();

    void setDisplayMode(@NonNull SerializableDisplayMode mode) throws Exception;

    void setIcon(java.nio.file.Path imagePath);

    void restore();

    void minimize();

    void show();

    void focus();

    void makeCurrent() throws Exception;

    boolean isFullscreen();

    /**
     * Returns the physical size of the monitor in millimeters.
     *
     * @return Vector2f [width, height] in mm
     */
    @NonNull
    Vector2f getMonitorPhysicalSize();

    /**
     * Returns the content scale of the monitor.
     *
     * @return Vector2f [xScale, yScale]
     */
    @NonNull
    Vector2f getMonitorContentScale();

    /**
     * Returns the content scale of the window.
     *
     * @return Vector2f [xScale, yScale]
     */
    @NonNull
    Vector2f getWindowContentScale();

    /**
     * Returns the pixel density of the window (Retina/High-DPI factor).
     *
     * @return float density
     */
    float getPixelDensity();

    /**
     * Returns true if the given display mode matches a native hardware exclusive fullscreen mode.
     *
     * @param mode the display mode to check
     * @return true if it corresponds to an exclusive fullscreen mode
     */
    boolean isExclusiveFullscreenMode(@NonNull SerializableDisplayMode mode);

    /**
     * Returns true if the window is currently in native hardware exclusive fullscreen mode.
     *
     * @return true if exclusive fullscreen is active
     */
    boolean isExclusiveFullscreen();

    /**
     * Updates the OS system UI visibility (e.g. taskbar, dock, menu bar) based on the game state.
     *
     * @param playing true if the game simulation is active (not in a menu)
     */
    void updateSystemUI(boolean playing);
}
