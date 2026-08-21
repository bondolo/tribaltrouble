package com.oddlabs.tt.gui.event;

import com.oddlabs.tt.gui.MouseButton;

public interface MouseMotionListener extends EventListener {
    void mouseDragged(MouseButton button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y);

    void mouseMoved(int x, int y);

    void mouseEntered();

    void mouseExited();
}
