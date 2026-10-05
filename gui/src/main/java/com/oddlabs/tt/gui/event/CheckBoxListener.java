package com.oddlabs.tt.gui.event;

/** Listener for checkbox state changes. */
@FunctionalInterface
public interface CheckBoxListener extends EventListener {
    void checked(boolean marked);
}
