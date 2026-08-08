package com.oddlabs.tt.client.guievent;

import com.oddlabs.tt.client.input.InputEvent;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface InputListener extends EventListener {
    void handleInput(@NonNull InputEvent event);
}
