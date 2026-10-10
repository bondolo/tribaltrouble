package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.engine.render.state.RenderContext;
import org.joml.Matrix4fc;

/**
 * Renders an individual sub-scene element or component within a broader render pass.
 */
public interface SubSceneRenderer {
    void render(RenderContext context, CameraState state, Matrix4fc modelView);
}
