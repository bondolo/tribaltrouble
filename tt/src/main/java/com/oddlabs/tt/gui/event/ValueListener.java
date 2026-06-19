package com.oddlabs.tt.gui.event;

@FunctionalInterface
public interface ValueListener extends EventListener {
    void valueSet(long value);
}
