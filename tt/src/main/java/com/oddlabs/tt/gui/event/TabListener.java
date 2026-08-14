package com.oddlabs.tt.gui.event;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface TabListener extends EventListener {
    void tabPressed(@NonNull String @NonNull [] words);
}
