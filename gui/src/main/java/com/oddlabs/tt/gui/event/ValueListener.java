package com.oddlabs.tt.gui.event;

/** Listener for numeric value updates in value-holding controls. */
@FunctionalInterface
public interface ValueListener extends EventListener {
    void valueSet(long value);
}
