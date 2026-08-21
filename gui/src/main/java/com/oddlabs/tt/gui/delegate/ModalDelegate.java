package com.oddlabs.tt.gui.delegate;

import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.InputEvent;

/**
 * Modal delegate layer that captures keyboard and mouse input.
 */
public final class ModalDelegate extends GUIObject implements InputDelegate {
    public ModalDelegate() {
        setPos(0, 0);
        setCanFocus(true);
    }

    @Override
    protected void displayChangedNotify(int width, int height) {
        setDim(width, height);
    }

    @Override
    public void handleInput(InputEvent event) {
        // Bubble
    }

    @Override
    public void mouseScrolled(int amount) {
    }

    @Override
    public void mouseMoved(int x, int y) {
    }

    @Override
    public void mouseDragged(MouseButton button, int x, int y, int relative_x, int relative_y, int absolute_x,
            int absolute_y) {
    }

    @Override
    public void mousePressed(MouseButton button, int x, int y) {
    }

    @Override
    public void mouseReleased(MouseButton button, int x, int y) {
    }
}
