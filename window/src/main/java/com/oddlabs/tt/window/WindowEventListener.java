package com.oddlabs.tt.window;

import org.jspecify.annotations.NonNull;
import org.lwjgl.sdl.SDL_Event;

/**
 * Listener interface for receiving raw windowing and input events from the window.
 */
public interface WindowEventListener {
    /**
     * Handles an SDL event dispatched by the window event loop.
     *
     * @param event the native SDL event
     */
    void handleSDLEvent(@NonNull SDL_Event event);

    /**
     * Called when the window gains OS focus.
     */
    default void onFocusGained() {
    }

    /**
     * Called when the window framebuffer size changes.
     *
     * @param width new framebuffer width in pixels
     * @param height new framebuffer height in pixels
     */
    default void onResized(int width, int height) {
    }

    /**
     * Called when a window-level toggle fullscreen action is requested (e.g. Alt+Enter).
     */
    default void onToggleFullscreen() {
    }
}
