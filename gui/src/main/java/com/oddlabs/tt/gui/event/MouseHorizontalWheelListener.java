package com.oddlabs.tt.gui.event;


/** Listener for horizontal mouse wheel scrolling events. */
@FunctionalInterface
public interface MouseHorizontalWheelListener extends EventListener {
    void mouseScrolledHorizontally(int amount);
}
