package com.oddlabs.tt.client.guievent;

@FunctionalInterface
public interface MouseWheelListener extends EventListener {
    void mouseScrolled(int amount);
}
