package com.oddlabs.tt.client.guievent;

@FunctionalInterface
public interface FocusListener extends EventListener {
    void activated(boolean activated);
}
