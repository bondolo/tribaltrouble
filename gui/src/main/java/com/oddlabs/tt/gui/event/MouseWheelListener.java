package com.oddlabs.tt.gui.event;

/** Listener for vertical mouse wheel scrolling events. */
@FunctionalInterface
public interface MouseWheelListener extends EventListener {
    void mouseScrolled(int amount);
}
