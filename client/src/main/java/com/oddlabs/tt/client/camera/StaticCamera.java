package com.oddlabs.tt.client.camera;

import com.oddlabs.tt.engine.render.CameraState;


public class StaticCamera extends Camera {
    public StaticCamera(CameraState camera) {
        super(null, camera);
    }

    @Override
    public void doAnimate(float t) {
    }
}
