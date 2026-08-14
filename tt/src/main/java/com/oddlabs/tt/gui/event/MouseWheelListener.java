package com.oddlabs.tt.gui.event;

@FunctionalInterface
public interface MouseWheelListener extends EventListener {
    void mouseScrolled(int amount);
}
