package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.client.render.*;
import com.oddlabs.tt.effects.render.*;

import com.oddlabs.tt.engine.render.state.RenderContext;
import org.jspecify.annotations.NonNull;

public abstract class ShadowListRenderer extends ShadowRenderer {
    public abstract void renderShadows(@NonNull RenderContext context, @NonNull RenderQueues queues,
            @NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack,
            @NonNull MatrixStack projectionStack);
}
