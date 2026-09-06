package com.oddlabs.tt.client.delegate;

import com.oddlabs.tt.client.camera.NullCamera;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.InputEvent;

/**
 * Null camera delegate providing an empty delegate state for camera views.
 */
public final class NullDelegate extends CameraDelegate<NullCamera> {
    private final boolean render_cursor;

    public NullDelegate(GUIRoot gui_root, boolean render_cursor) {
        super(gui_root, new NullCamera());
        this.render_cursor = render_cursor;
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
    public final boolean renderCursor() {
        return render_cursor;
    }
}
