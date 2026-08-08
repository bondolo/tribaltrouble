package com.oddlabs.tt.client.guievent;

@FunctionalInterface
public interface ValueListener extends EventListener {
    void valueSet(long value);
}
