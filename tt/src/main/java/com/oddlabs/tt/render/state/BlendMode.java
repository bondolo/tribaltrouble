package com.oddlabs.tt.render.state;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 * Enumeration of common OpenGL blend modes used in the renderer.
 * Each mode provides an apply method to set the corresponding GL state.
 */
public enum BlendMode implements Mode {
    NONE {
        @Override
        public void apply() {
            GL11.glDisable(GL11.GL_BLEND);
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        }
    },
    ALPHA {
        @Override
        public void apply() {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        }
    },
    ADDITIVE {
        @Override
        public void apply() {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        }
    },
    PREMULTIPLIED {
        @Override
        public void apply() {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        }
    },
    MAX {
        @Override
        public void apply() {
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendEquation(GL14.GL_MAX);
        }
    },
    CUSTOM {
        @Override
        public void apply() {
            // Managed manually via setBlendFunc
        }
    };
}
