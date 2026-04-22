package com.oddlabs.tt.vbo;

import com.oddlabs.tt.render.state.RenderContext;
import org.jspecify.annotations.NonNull;
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
        GL30.glBindVertexArray(id);
    }

    public void bind(@NonNull RenderContext context) {
        context.bindVertexArray(id);
    }

    public void unbind() {
        GL30.glBindVertexArray(0);
    }

    public void unbind(@NonNull RenderContext context) {
        context.bindVertexArray(0);
    }

    @Override
    public void close() {
        GL30.glDeleteVertexArrays(id);
    }
}
