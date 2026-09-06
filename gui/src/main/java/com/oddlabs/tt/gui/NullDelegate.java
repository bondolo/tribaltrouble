package com.oddlabs.tt.gui;

import com.oddlabs.tt.gui.delegate.InputDelegate;
import com.oddlabs.tt.input.InputEvent;

/**
 * Null input delegate representing an empty or inactive delegate state.
 */
final class NullDelegate extends GUIObject implements InputDelegate {
    private final boolean renderCursor;

    NullDelegate(boolean renderCursor) {
        this.renderCursor = renderCursor;
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

    @Override
    public boolean renderCursor() {
        return renderCursor;
    }
}
