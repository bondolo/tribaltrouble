package com.oddlabs.tt.client;

import com.oddlabs.tt.engine.render.FrameDriver;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Functional interface for initializing client systems, frame driver, and returning an optional background load task.
 */
@FunctionalInterface
public interface ClientStartup {
    /**
     * Container holding the initialized frame driver and optional background load task.
     *
     * @param driver the active frame driver
     * @param loadTask optional background runnable to execute after the first frame
     */
    record Session(@NonNull FrameDriver driver, @Nullable Runnable loadTask) {
    }

    /**
     * Initializes the client after the engine and window/GL context are initialized.
     *
     * @param engine the active client engine
     * @param firstProgress whether this is the initial application load
     * @return client session holding the frame driver and load task
     */
    @NonNull
    Session init(@NonNull Peer engine, boolean firstProgress);
}
