package com.oddlabs.tt.gui.event;

import com.oddlabs.tt.gui.PulldownMenu;

/** Listener for item selection in pulldown menus. */
@FunctionalInterface
public interface ItemChosenListener<T> extends EventListener {
    void itemChosen(PulldownMenu<T> menu, int item_index);
}
