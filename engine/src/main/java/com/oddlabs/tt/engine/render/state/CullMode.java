package com.oddlabs.tt.engine.render.state;

import org.lwjgl.opengl.GL11;

/**
 * Polygon culling modes for rendering geometry.
 */
public enum CullMode {
    NONE {
        @Override
        void apply(RenderContext context) {
            context.setCullFace(false);
        }
    },
    BACK {
        @Override
        void apply(RenderContext context) {
            context.setCullFace(true);
            context.setCullFaceMode(GL11.GL_BACK);
        }
    },
    FRONT {
        @Override
        void apply(RenderContext context) {
            context.setCullFace(true);
            context.setCullFaceMode(GL11.GL_FRONT);
        }
    };

    abstract void apply(RenderContext context);
}
