package com.oddlabs.tt.client.guievent;

@FunctionalInterface
public interface CheckBoxListener extends EventListener {
    void checked(boolean marked);
}
