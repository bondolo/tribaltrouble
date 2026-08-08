package com.oddlabs.tt.client.guievent;

import com.oddlabs.tt.client.gui.MouseButton;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface MouseClickListener extends EventListener {
    void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks);
}
