package com.oddlabs.tt.gui.event;


public interface RowListener<T> extends EventListener {
    default void rowDoubleClicked(T row_context) {
    }

    default void rowChosen(T row_context) {
    }
}
