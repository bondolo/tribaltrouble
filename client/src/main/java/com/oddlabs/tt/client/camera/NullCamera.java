package com.oddlabs.tt.client.camera;

import com.oddlabs.tt.engine.render.CameraState;


public final class NullCamera extends Camera {
    public NullCamera() {
        super(null, new CameraState());
    }

    @Override
    public void doAnimate(float t) {
    }
}
