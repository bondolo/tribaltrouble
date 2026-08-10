package com.oddlabs.tt.client.camera;

import com.oddlabs.tt.engine.render.CameraState;


import org.jspecify.annotations.NonNull;

public class StaticCamera extends Camera {
    public StaticCamera(@NonNull CameraState camera) {
        super(null, camera);
    }

    @Override
    public void doAnimate(float t) {
    }
}
