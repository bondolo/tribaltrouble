package com.oddlabs.tt.engine.window;

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
}
