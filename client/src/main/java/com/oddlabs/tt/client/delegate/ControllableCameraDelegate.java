package com.oddlabs.tt.client.delegate;

import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.gui.event.EventListener;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.client.viewer.WorldViewer;

public abstract class ControllableCameraDelegate<C extends Camera> extends InGameDelegate<C> implements EventListener {
    private FirstPersonDelegate first_person_delegate;

    public ControllableCameraDelegate(WorldViewer viewer, C camera) {
        super(viewer, camera);
    }

    @Override
    public void handleInput(InputEvent event) {
        super.handleInput(event);
        if (event.isConsumed()) return;

        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            if (event.consumeAction(GameAction.CAMERA_FIRST_PERSON)) {
                pushFirstPersonDelegate(true);
                event.consume();
                return;
            }
            if (event.consumeAction(GameAction.CAMERA_ZOOM_MODE)) {
                pushZoomDelegate();
                event.consume();
                return;
            }
        }
    }

    @Override
    public void mousePressed(MouseButton button, int x, int y) {
        if (button == MouseButton.MIDDLE) {
            pushFirstPersonDelegate(false);
        }
    }

    @Override
    public void mouseReleased(MouseButton button, int x, int y) {
        if (button == MouseButton.MIDDLE && first_person_delegate != null) {
            first_person_delegate.mouseReleased(button, x, y);
        }
    }

    @Override
    public void mouseScrolled(int amount) {
        getCamera().mouseScrolled(amount);
    }

    @Override
    public void mouseScrolledHorizontally(int amount) {
        getCamera().rotate(amount);
    }

    @Override
    public void mouseMoved(int x, int y) {
        getCamera().mouseMoved(x, y);
    }

    @Override
    public final boolean canScroll() {
        var guiRoot = getGUIRoot();
        float scale = guiRoot.getGlobalScale();
        mouseMoved(Math.round(guiRoot.getMouseX() / scale), Math.round(guiRoot.getMouseY() / scale));
        return guiRoot.getModalDelegate() == null;
    }

    @Override
    public void mouseDragged(MouseButton button, int x, int y, int relative_x, int relative_y, int absolute_x,
            int absolute_y) {
        if (button == MouseButton.MIDDLE && first_person_delegate != null) {
            first_person_delegate.mouseDragged(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
        }
    }

    private void pushFirstPersonDelegate(boolean key_pressed) {
        first_person_delegate = new FirstPersonDelegate(getViewer(), getCamera().getState(), key_pressed);
        getGUIRoot().pushDelegate(first_person_delegate);
    }

    protected void pushZoomDelegate() {
    }
}
