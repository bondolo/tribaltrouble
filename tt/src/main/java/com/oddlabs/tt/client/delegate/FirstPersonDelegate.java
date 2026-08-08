package com.oddlabs.tt.client.delegate;

import com.oddlabs.tt.client.camera.CameraState;
import com.oddlabs.tt.client.camera.FirstPersonCamera;
import com.oddlabs.tt.client.gui.CursorType;
import com.oddlabs.tt.client.gui.MouseButton;
import com.oddlabs.tt.client.input.GameAction;
import com.oddlabs.tt.client.input.InputEvent;
import com.oddlabs.tt.client.input.InputPhase;
import com.oddlabs.tt.engine.render.Renderer;
import com.oddlabs.tt.client.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

public class FirstPersonDelegate extends InGameDelegate<FirstPersonCamera> {
    private final boolean key_pressed;
    private final int created_tick;

    private boolean done = false;

    public FirstPersonDelegate(@NonNull WorldViewer viewer, @NonNull CameraState camera_state, boolean key_pressed) {
        super(viewer, new FirstPersonCamera(viewer, viewer.getWorld().getHeightMap(), camera_state));
        this.key_pressed = key_pressed;
        created_tick = Renderer.getRenderer().getEventQueue().getManager().getTick();
    }

    private void release() {
        done = true;
    }

    @Override
    public final void doRemove() {
        super.doRemove();
        if (!done) {
            release();
        }
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        super.handleInput(event);
        if (event.isConsumed()) return;

        if (event.getPhase() == InputPhase.RELEASED) {
            if (key_pressed && !done) {
                if (event.consumeAction(GameAction.CAMERA_FIRST_PERSON)) {
                    pop();
                }
            }
        }
        // Consume everything (modal-ish behavior for first person control)
        event.consume();
    }

    @Override
    public void mouseScrolled(int amount) {
    }

    @Override
    public void mouseMoved(int x, int y) {
        if (!done)
            getCamera().mouseMoved(x, y);
    }

    @Override
    public void mouseDragged(@NonNull MouseButton button, int x, int y, int relative_x, int relative_y, int absolute_x,
            int absolute_y) {
        if (created_tick == Renderer.getRenderer().getEventQueue().getManager().getTick())
            return;
        if ((button == MouseButton.MIDDLE || key_pressed) && !done && getGUIRoot().getModalDelegate() == null) {
            getCamera().mouseMoved(x, y);
        }
    }

    @Override
    public void mousePressed(@NonNull MouseButton button, int x, int y) {
    }

    @Override
    public void mouseReleased(@NonNull MouseButton button, int x, int y) {
        if (button == MouseButton.MIDDLE && !key_pressed && !done) {
            pop();
        }
    }

    @Override
    protected @NonNull CursorType getCursorType() {
        return CursorType.NULL;
    }
}
