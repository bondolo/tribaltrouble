package com.oddlabs.tt.gui.event;


/** Listener for enter key submissions in text input controls. */
@FunctionalInterface
public interface EnterListener extends EventListener {
    void enterPressed(CharSequence text);
}
