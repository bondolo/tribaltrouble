package com.oddlabs.tt.engine.vbo;

import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;

/**
 * Byte array vertex buffer object.
 */
public final class ByteVBO extends VBO {

    public ByteVBO(int usage, int size) {
        super(GL15.GL_ARRAY_BUFFER, usage, size);
    }

    public ByteVBO(int usage, ByteBuffer initial_data) {
        this(usage, initial_data.remaining());
        put(initial_data);
    }

    public void put(ByteBuffer buffer) {
        bind();
        GL15.glBufferSubData(getTarget(), 0, buffer);
        buffer.position(buffer.limit());
    }

    @Override
    public int capacity() {
        return getSize();
    }
}
