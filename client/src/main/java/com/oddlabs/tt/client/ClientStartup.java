package com.oddlabs.tt.client;

import com.oddlabs.tt.gui.GUI;
import org.jspecify.annotations.Nullable;

/**
 * Initializes client systems and GUI, returning an optional background load task.
 */
@FunctionalInterface
public interface ClientStartup {
    /**
     * Container holding the initialized GUI and optional background load task.
     *
     * @param gui the active GUI
     * @param loadTask optional background runnable to execute after the first frame
     */
    record Session(GUI gui, @Nullable Runnable loadTask) {
    }

    /**
     * Initializes the client after the engine and window/GL context are initialized.
     *
     * @param engine the active client engine
     * @param firstProgress whether this is the initial application load
     * @return client session holding the GUI and load task
     */
    Session init(Peer engine, boolean firstProgress);
}
