package com.oddlabs.tt.gui.event;

import com.oddlabs.tt.gui.MouseButton;

public interface MouseButtonListener extends MouseClickListener {
    void mousePressed(MouseButton button, int x, int y);

    void mouseReleased(MouseButton button, int x, int y);

    void mouseHeld(MouseButton button, int x, int y);
}
