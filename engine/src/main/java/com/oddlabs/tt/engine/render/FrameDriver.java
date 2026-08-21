package com.oddlabs.tt.engine.render;

import com.oddlabs.net.NetworkSelector;

/**
 * Interface for driving input, game ticks, and rendering for a display frame.
 */
public interface FrameDriver {
    /**
     * Polls input and advances the local game tick.
     *
     * @param network the active network selector
     */
    void tick(NetworkSelector network);

    /**
     * Renders the 3D scene and 2D overlay for the current frame.
     */
    void render();

    /**
     * Performs mouse hover picking if not frozen.
     */
    void pickHover();

    /**
     * Handles the window close request event.
     */
    void onCloseRequested();

    /**
     * Executes the session loop within the driver's execution context.
     *
     * @param session the session loop to execute
     */
    default void run(Runnable session) {
        session.run();
    }
}
