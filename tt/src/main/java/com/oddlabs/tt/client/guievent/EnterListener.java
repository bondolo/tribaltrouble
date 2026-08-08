package com.oddlabs.tt.client.guievent;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface EnterListener extends EventListener {
    void enterPressed(@NonNull CharSequence text);
}
