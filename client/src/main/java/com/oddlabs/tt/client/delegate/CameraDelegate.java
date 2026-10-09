package com.oddlabs.tt.client.delegate;

import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.input.InputEvent;
import org.joml.Matrix4f;

import java.util.Objects;

/**
 * Bridges user interface input events and viewport projection with an active camera.
 */
public abstract class CameraDelegate<C extends Camera> extends Delegate {
    private final GUIRoot gui_root;
    private C camera;

    public CameraDelegate(GUIRoot gui_root, C camera) {
        this.camera = Objects.requireNonNull(camera);
        this.gui_root = gui_root;
    }

    public final GUIRoot getGUIRoot() {
        return gui_root;
    }

    public final void setCamera(C camera) {
        this.camera = Objects.requireNonNull(camera);
    }

    public final C getCamera() {
        return camera;
    }

    @Override
    public final CameraState getCameraState() {
        return camera.getState();
    }

    @Override
    public void updateView(int width, int height) {
        camera.updateView(width, height);
    }

    @Override
    public Matrix4f multProjection(Matrix4f matrix, int width, int height) {
        return camera.applyPerspective(matrix, width, height);
    }

    @Override
    public void handleInput(InputEvent event) {
        camera.handleInput(event);
        if (!event.isConsumed()) {
            super.handleInput(event);
        }
    }

    @Override
    protected void doAdd() {
        super.doAdd();
        getCamera().enable();
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        getCamera().disable();
    }

    @Override
    public boolean renderCursor() {
        return true;
    }

    @Override
    public boolean canScroll() {
        return false;
    }

    @Override
    public boolean forceRender() {
        return false;
    }

    public final void pop() {
        gui_root.removeDelegate(this);
    }
}
