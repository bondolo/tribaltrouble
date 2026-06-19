package com.oddlabs.tt.gui.event;

import org.jspecify.annotations.NonNull;

public interface RowListener<T> extends EventListener {
    default void rowDoubleClicked(@NonNull T row_context) {
    }

    default void rowChosen(@NonNull T row_context) {
    }
}
