package com.oddlabs.tt.gui.event;

@FunctionalInterface
public interface CloseListener extends EventListener {
    void closed();
}
