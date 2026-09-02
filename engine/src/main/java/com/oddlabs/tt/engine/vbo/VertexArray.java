package com.oddlabs.tt.engine.vbo;

import com.oddlabs.tt.engine.render.state.RenderContext;
import org.lwjgl.opengl.GL30;

/**
 * An OpenGL Vertex Array Object (VAO).
 */
public final class VertexArray implements AutoCloseable {
    private final int id;

    public VertexArray() {
        this.id = GL30.glGenVertexArrays();
    }

    public void bind() {
        RenderContext.current().bindVertexArray(id);
    }

    public void unbind() {
        RenderContext.current().bindVertexArray(0);
    }

    @Override
    public void close() {
        if (id != 0) {
            RenderContext.current().invalidateVertexArray(id);
            GL30.glDeleteVertexArrays(id);
        }
    }
}
