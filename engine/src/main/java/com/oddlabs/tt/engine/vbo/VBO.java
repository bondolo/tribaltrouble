package com.oddlabs.tt.engine.vbo;

import com.oddlabs.tt.base.resource.NativeResource;
import com.oddlabs.tt.engine.render.state.RenderContext;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

/**
 * Base abstract wrapper for OpenGL Vertex Buffer Objects (VBOs).
 */
public abstract class VBO extends NativeResource<VBO.Buffer> {
    static final class Buffer extends NativeResource.NativeState {

        private final int handle;

        Buffer(int target, int usage, int size) {
            this.handle = createBuffer(target, usage, size);
        }

        private static int createBuffer(int target, int usage, int size) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer handle_buffer = stack.mallocInt(1);
                GL15.glGenBuffers(handle_buffer);
                int handle = handle_buffer.get(0);
                assert handle != 0;
                RenderContext.current().bindBuffer(target, handle);
                GL15.glBufferData(target, size, usage);
                return handle;
            }
        }

        @Override
        public void close() {
            RenderContext.current().invalidateBuffer(handle);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer handle_buffer = stack.mallocInt(1);
                handle_buffer.put(0, handle);
                GL15.glDeleteBuffers(handle_buffer);
            }
        }
    }

    private final int target;
    private final int size;

    public static void releaseAll(RenderContext context) {
        context.bindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        releaseIndexVBO(context);
    }

    public static void releaseIndexVBO(RenderContext context) {
        context.bindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public VBO(int target, int usage, int size) {
        super(new Buffer(target, usage, size));
        this.target = target;
        this.size = size;
    }

    public final void bind() {
        bind(RenderContext.current());
    }

    public final void bind(RenderContext context) {
        context.bindBuffer(target, state.handle);
    }

    protected final int getTarget() {
        return target;
    }

    protected final int getSize() {
        return size;
    }

    public abstract int capacity();

    public int getHandle() {
        return state.handle;
    }
}
