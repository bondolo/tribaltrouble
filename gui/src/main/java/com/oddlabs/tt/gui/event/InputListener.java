package com.oddlabs.tt.gui.event;

import com.oddlabs.tt.input.InputEvent;

@FunctionalInterface
public interface InputListener extends EventListener {
    void handleInput(InputEvent event);
}
