package com.oddlabs.tt.gui.event;


@FunctionalInterface
public interface EnterListener extends EventListener {
    void enterPressed(CharSequence text);
}
