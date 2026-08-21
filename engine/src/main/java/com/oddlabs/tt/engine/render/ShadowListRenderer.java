package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.engine.render.state.RenderContext;

public abstract class ShadowListRenderer extends ShadowRenderer {
    public abstract void renderShadows(RenderContext context, RenderQueues queues,
            LandscapeRenderer renderer, MatrixStack modelViewStack,
            MatrixStack projectionStack);
}
