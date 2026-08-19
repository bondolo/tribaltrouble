package com.oddlabs.tt.gui.delegate;

import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.InputEvent;
import org.jspecify.annotations.NonNull;

/**
 * Null input delegate representing an empty or inactive delegate state.
 */
public class NullDelegate extends GUIObject implements InputDelegate {
    private final boolean renderCursor;

    public NullDelegate(boolean renderCursor) {
        this.renderCursor = renderCursor;
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

    @Override
    public boolean renderCursor() {
        return renderCursor;
    }
}
