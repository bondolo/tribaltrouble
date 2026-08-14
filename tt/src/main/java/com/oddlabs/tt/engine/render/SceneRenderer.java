package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.engine.render.state.RenderContext;
import org.jspecify.annotations.NonNull;

public interface SceneRenderer {
    void render(@NonNull RenderContext context, @NonNull CameraState state, @NonNull MatrixStack modelView,
            @NonNull MatrixStack projection);
}
