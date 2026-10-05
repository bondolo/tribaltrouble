package com.oddlabs.tt.gui.event;

/** Listener for window or dialog close events. */
@FunctionalInterface
public interface CloseListener extends EventListener {
    void closed();
}
