package com.oddlabs.tt.client.guievent;


@FunctionalInterface
public interface MouseHorizontalWheelListener extends EventListener {
    void mouseScrolledHorizontally(int amount);
}
