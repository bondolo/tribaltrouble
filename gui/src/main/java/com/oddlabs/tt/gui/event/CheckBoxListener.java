package com.oddlabs.tt.gui.event;

@FunctionalInterface
public interface CheckBoxListener extends EventListener {
    void checked(boolean marked);
}
