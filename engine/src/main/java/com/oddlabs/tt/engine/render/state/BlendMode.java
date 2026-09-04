package com.oddlabs.tt.engine.render.state;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 * Blend modes and equations for rendering passes.
 */
public enum BlendMode {
    NONE {
        @Override
        void apply(RenderContext context) {
            context.setBlend(false);
            context.setBlendEquation(GL14.GL_FUNC_ADD);
        }
    },
    ALPHA {
        @Override
        void apply(RenderContext context) {
            context.setBlend(true);
            context.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            context.setBlendEquation(GL14.GL_FUNC_ADD);
        }
    },
    ADDITIVE {
        @Override
        void apply(RenderContext context) {
            context.setBlend(true);
            context.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            context.setBlendEquation(GL14.GL_FUNC_ADD);
        }
    },
    PREMULTIPLIED {
        @Override
        void apply(RenderContext context) {
            context.setBlend(true);
            context.setBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            context.setBlendEquation(GL14.GL_FUNC_ADD);
        }
    },
    MAX {
        @Override
        void apply(RenderContext context) {
            context.setBlend(true);
            context.setBlendEquation(GL14.GL_MAX);
        }
    },
    CUSTOM {
        @Override
        void apply(RenderContext context) {
            // Managed manually via setBlendFunc
        }
    };

    abstract void apply(RenderContext context);
}
