package com.oddlabs.tt.gui.event;

import com.oddlabs.tt.input.InputEvent;

/** Listener for raw input event handling on GUI objects. */
@FunctionalInterface
public interface InputListener extends EventListener {
    void handleInput(InputEvent event);
}
