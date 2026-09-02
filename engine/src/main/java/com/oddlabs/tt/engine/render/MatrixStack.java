package com.oddlabs.tt.engine.render;


import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Maintains a stack of transformation matrix that are applied to the drawing.
 */
public final class MatrixStack {
    private final Deque<Matrix4f> stack = new ArrayDeque<>();

    public interface TopListener {
        void topChanging(Matrix4fc matrix);
    }

    private final @Nullable TopListener topListener;

    public MatrixStack() {
        this(null);
    }

    public MatrixStack(@Nullable TopListener topListener) {
        clear();
        this.topListener = topListener;
    }

    public Matrix4f push() {
        Matrix4f copy = new Matrix4f(current());
        stack.push(copy);
        if (null != topListener) {
            topListener.topChanging(current());
        }
        return current();
    }

    public Matrix4f pop() {
        if (null != topListener) {
            topListener.topChanging(current());
        }
        if (stack.size() > 1) {
            stack.pop();
        } else {
            clear();
        }
        return current();
    }

    public Matrix4f current() {
        return stack.element();
    }

    public Matrix4f clear() {
        stack.clear();
        stack.push(new Matrix4f());

        return current();
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
