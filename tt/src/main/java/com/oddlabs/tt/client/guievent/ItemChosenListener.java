package com.oddlabs.tt.client.guievent;

import com.oddlabs.tt.client.gui.PulldownMenu;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface ItemChosenListener<T> extends EventListener {
    void itemChosen(@NonNull PulldownMenu<T> menu, int item_index);
}
