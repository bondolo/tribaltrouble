package com.oddlabs.tt.gui.event;


/** Listener for row selection and interaction in row-based controls. */
public interface RowListener<T> extends EventListener {
    default void rowDoubleClicked(T row_context) {
    }

    default void rowChosen(T row_context) {
    }
}
