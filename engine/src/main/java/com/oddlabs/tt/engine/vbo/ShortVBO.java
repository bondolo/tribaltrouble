package com.oddlabs.tt.engine.vbo;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import java.nio.ShortBuffer;

/**
 * Short index array vertex buffer object.
 */
public final class ShortVBO extends VBO {

    public ShortVBO(int usage, int size) {
        super(GL15.GL_ELEMENT_ARRAY_BUFFER, usage, size * Short.BYTES);
    }

    public ShortVBO(int usage, ShortBuffer initial_data) {
        this(usage, initial_data.remaining());
        put(initial_data);
    }

    public void put(ShortBuffer buffer) {
        bind();
        GL15.glBufferSubData(getTarget(), 0, buffer);
        buffer.position(buffer.limit());
    }

    public void drawElements(int mode, int count, int index) {
        bind();
        GL11.glDrawElements(mode, count, GL11.GL_UNSIGNED_SHORT, index << 1);
    }

    public void drawElementsInstanced(int mode, int count, int index, int primcount) {
        bind();
        GL31.glDrawElementsInstanced(mode, count, GL11.GL_UNSIGNED_SHORT, index << 1, primcount);
    }

    @Override
    public int capacity() {
        return getSize() / Short.BYTES;
    }
}
