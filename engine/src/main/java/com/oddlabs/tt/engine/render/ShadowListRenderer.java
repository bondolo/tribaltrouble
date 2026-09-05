package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.engine.render.state.RenderContext;

/**
 * Renders batches of shadows stored in lists or queues.
 */
public abstract class ShadowListRenderer extends ShadowRenderer {
    public abstract void renderShadows(RenderContext context, RenderQueues queues,
            float worldSize, Texture heightTexture, MatrixStack modelViewStack,
            MatrixStack projectionStack);
}
