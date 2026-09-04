package com.oddlabs.tt.engine.render.state;

/**
 * Depth testing and writing configurations for rendering passes.
 */
public enum DepthMode {
    NONE {
        @Override
        void apply(RenderContext context) {
            context.setDepthTest(false);
        }
    },
    READ_ONLY {
        @Override
        void apply(RenderContext context) {
            context.setDepthTest(true);
            context.setDepthMask(false);
        }
    },
    READ_WRITE {
        @Override
        void apply(RenderContext context) {
            context.setDepthTest(true);
            context.setDepthMask(true);
        }
    };

    abstract void apply(RenderContext context);
}
