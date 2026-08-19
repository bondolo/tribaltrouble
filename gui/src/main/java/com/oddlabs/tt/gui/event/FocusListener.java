package com.oddlabs.tt.gui.event;

@FunctionalInterface
public interface FocusListener extends EventListener {
    void activated(boolean activated);
}
