package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.engine.render.state.RenderContext;

public interface SceneRenderer {
    void render(RenderContext context, CameraState state, MatrixStack modelView,
            MatrixStack projection);
}
