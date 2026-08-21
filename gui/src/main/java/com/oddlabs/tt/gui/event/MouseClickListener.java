package com.oddlabs.tt.gui.event;

import com.oddlabs.tt.gui.MouseButton;

@FunctionalInterface
public interface MouseClickListener extends EventListener {
    void mouseClicked(MouseButton button, int x, int y, int clicks);
}
