package com.oddlabs.tt.client;

import com.oddlabs.tt.client.screen.ScreenManager;
import com.oddlabs.tt.gui.GUI;
import org.jspecify.annotations.Nullable;

/**
 * Initializes client systems and active screen manager, returning an optional background load task.
 */
@FunctionalInterface
public interface ClientStartup {
    /**
     * Container holding the initialized screen manager and optional background load task.
     *
     * @param screenManager the active screen manager
     * @param loadTask optional background runnable to execute after the first frame
     */
    record Session(ScreenManager screenManager, @Nullable Runnable loadTask) implements AutoCloseable {
        public GUI gui() {
            return screenManager.getGUI();
        }

        @Override
        public void close() {
            screenManager.close();
        }
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
