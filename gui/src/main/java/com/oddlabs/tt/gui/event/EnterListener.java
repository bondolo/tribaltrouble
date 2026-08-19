package com.oddlabs.tt.gui.event;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface EnterListener extends EventListener {
    void enterPressed(@NonNull CharSequence text);
}
