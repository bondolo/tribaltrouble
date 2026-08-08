package com.oddlabs.tt.client.guievent;

@FunctionalInterface
public interface CloseListener extends EventListener {
    void closed();
}
