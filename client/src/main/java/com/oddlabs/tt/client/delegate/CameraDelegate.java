package com.oddlabs.tt.client.delegate;

import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.input.InputEvent;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public abstract class CameraDelegate<C extends Camera> extends Delegate {
    private final GUIRoot gui_root;
    private @Nullable C camera;

    public CameraDelegate(GUIRoot gui_root, @Nullable C camera) {
        this.camera = camera;
        this.gui_root = gui_root;
    }

    public final GUIRoot getGUIRoot() {
        return gui_root;
    }

    public final void setCamera(C camera) {
        this.camera = camera;
    }

    public final C getCamera() {
        if (camera == null) {
            throw new IllegalStateException("Camera not set");
        }
        return camera;
    }

    @Override
    public final @Nullable CameraState getCameraState() {
        return camera != null ? camera.getState() : null;
    }

    @Override
    public Matrix4f multProjection(Matrix4f matrix, int width, int height) {
        if (camera != null && height > 0) {
            float aspect = (float) width / height;
            float fovy = Camera.calculateDynamicFOV(camera.getState().getCurrentZ(), aspect, Camera.FOVMode.DIAGONAL);
            float zNear = RenderConfig.VIEW_MIN;
            float zFar = RenderConfig.VIEW_MAX;
            Matrix4f perspectiveMatrix = new Matrix4f().perspective((float) Math.toRadians(fovy), aspect, zNear, zFar);
            return matrix.mul(perspectiveMatrix);
        }
        return matrix;
    }

    @Override
    public void handleInput(InputEvent event) {
        if (camera != null) {
            camera.handleInput(event);
        }
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
