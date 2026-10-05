package com.oddlabs.tt.gui.event;


/** Listener for tab key completion events in text input controls. */
@FunctionalInterface
public interface TabListener extends EventListener {
    void tabPressed(String[] words);
}
