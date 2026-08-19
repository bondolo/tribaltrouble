package com.oddlabs.tt.gui.event;


@FunctionalInterface
public interface MouseHorizontalWheelListener extends EventListener {
    void mouseScrolledHorizontally(int amount);
}
