package com.oddlabs.tt.gui.event;


@FunctionalInterface
public interface TabListener extends EventListener {
    void tabPressed(String[] words);
}
