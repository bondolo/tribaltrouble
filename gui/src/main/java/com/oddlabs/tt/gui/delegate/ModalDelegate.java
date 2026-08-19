package com.oddlabs.tt.gui.delegate;

import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.InputEvent;
import org.jspecify.annotations.NonNull;

/**
 * Modal delegate layer that captures keyboard and mouse input.
 */
public final class ModalDelegate extends GUIObject implements InputDelegate {
    public ModalDelegate() {
        setPos(0, 0);
        setCanFocus(true);
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        // Bubble
    }

    @Override
    public void mouseScrolled(int amount) {
    }

    @Override
    public void mouseMoved(int x, int y) {
    }

    @Override
    public void mouseDragged(@NonNull MouseButton button, int x, int y, int relative_x, int relative_y, int absolute_x,
            int absolute_y) {
    }

    @Override
    public void mousePressed(@NonNull MouseButton button, int x, int y) {
    }

    @Override
    public void mouseReleased(@NonNull MouseButton button, int x, int y) {
    }
}
