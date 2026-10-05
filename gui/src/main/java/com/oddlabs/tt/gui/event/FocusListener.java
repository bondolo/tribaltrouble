package com.oddlabs.tt.gui.event;

/** Listener for focus activation and deactivation events. */
@FunctionalInterface
public interface FocusListener extends EventListener {
    void activated(boolean activated);
}
