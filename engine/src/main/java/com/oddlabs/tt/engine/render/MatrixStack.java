package com.oddlabs.tt.engine.render;


import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/**
 * Maintains a stack of transformation matrix that are applied to the drawing.
 */
public final class MatrixStack {
    private static final int DEFAULT_CAPACITY = 32;

    private Matrix4f[] stack = new Matrix4f[DEFAULT_CAPACITY];
    private int top = 0;

    public interface TopListener {
        void topChanging(Matrix4fc matrix);
    }

    private final @Nullable TopListener topListener;

    public MatrixStack() {
        this(null);
    }

    public MatrixStack(@Nullable TopListener topListener) {
        for (int i = 0; i < stack.length; i++) {
            stack[i] = new Matrix4f();
        }
        this.topListener = topListener;
    }

    public Matrix4f push() {
        ensureCapacity(top + 2);
        top++;
        stack[top].set(stack[top - 1]);
        if (topListener != null) {
            topListener.topChanging(stack[top]);
        }
        return stack[top];
    }

    public Matrix4f pop() {
        if (top > 0) {
            top--;
        } else {
            clear();
        }
        if (topListener != null) {
            topListener.topChanging(stack[top]);
        }
        return stack[top];
    }

    public Matrix4f current() {
        return stack[top];
    }

    public Matrix4f clear() {
        top = 0;
        stack[0].identity();
        return stack[0];
    }

    public int size() {
        return top + 1;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > stack.length) {
            int newCapacity = stack.length * 2;
            Matrix4f[] newStack = new Matrix4f[newCapacity];
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            for (int i = stack.length; i < newCapacity; i++) {
                newStack[i] = new Matrix4f();
            }
            stack = newStack;
        }
    }

    public MatrixStack translate(float x, float y, float z) {
        current().translate(x, y, z);
        return this;
    }

    /**
     * Applies a rotation to the current matrix.
     *
     * @param angle The angle to rotate, in DEGREES.
     * @param x The x component of the rotation axis.
     * @param y The y component of the rotation axis.
     * @param z The z component of the rotation axis.
     * @return This matrix stack for chaining.
     */
    public MatrixStack rotate(float angle, float x, float y, float z) {
        current().rotate((float) Math.toRadians(angle), x, y, z);
        return this;
    }

    public MatrixStack scale(float x, float y, float z) {
        current().scale(x, y, z);
        return this;
    }

    public MatrixStack multiply(Matrix4fc matrix) {
        current().mul(matrix);
        return this;
    }
}
