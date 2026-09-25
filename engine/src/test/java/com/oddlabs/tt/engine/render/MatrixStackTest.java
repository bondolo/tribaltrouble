package com.oddlabs.tt.engine.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link MatrixStack}.
 */
final class MatrixStackTest {

    @Test
    void testInitialState() {
        MatrixStack stack = new MatrixStack();
        assertEquals(1, stack.size());
        assertEquals(new Matrix4f(), stack.current());
    }

    @Test
    void testPushAndPop() {
        MatrixStack stack = new MatrixStack();
        stack.translate(10f, 20f, 30f);

        stack.push();
        assertEquals(2, stack.size());

        stack.translate(5f, 5f, 5f);
        Vector3f pos = new Vector3f();
        stack.current().getTranslation(pos);
        assertEquals(15f, pos.x, 1e-5f);
        assertEquals(25f, pos.y, 1e-5f);
        assertEquals(35f, pos.z, 1e-5f);

        stack.pop();
        assertEquals(1, stack.size());
        stack.current().getTranslation(pos);
        assertEquals(10f, pos.x, 1e-5f);
        assertEquals(20f, pos.y, 1e-5f);
        assertEquals(30f, pos.z, 1e-5f);
    }

    @Test
    void testClear() {
        MatrixStack stack = new MatrixStack();
        stack.push();
        stack.push();
        stack.translate(1f, 2f, 3f);
        assertEquals(3, stack.size());

        stack.clear();
        assertEquals(1, stack.size());
        assertEquals(new Matrix4f(), stack.current());
    }

    @Test
    void testPopAtBottomClears() {
        MatrixStack stack = new MatrixStack();
        stack.translate(5f, 5f, 5f);
        assertEquals(1, stack.size());

        stack.pop();
        assertEquals(1, stack.size());
        assertEquals(new Matrix4f(), stack.current());
    }

    @Test
    void testCapacityExpansion() {
        MatrixStack stack = new MatrixStack();
        int pushCount = 40;
        for (int i = 0; i < pushCount; i++) {
            stack.push();
            stack.translate(1f, 0f, 0f);
        }
        assertEquals(pushCount + 1, stack.size());

        Vector3f pos = new Vector3f();
        stack.current().getTranslation(pos);
        assertEquals(40f, pos.x, 1e-5f);

        for (int i = 0; i < pushCount; i++) {
            stack.pop();
        }
        assertEquals(1, stack.size());
        stack.current().getTranslation(pos);
        assertEquals(0f, pos.x, 1e-5f);
    }

    @Test
    void testTopListenerNotification() {
        AtomicInteger notifications = new AtomicInteger();
        MatrixStack stack = new MatrixStack(_ -> notifications.incrementAndGet());

        stack.push();
        assertEquals(1, notifications.get());

        stack.pop();
        assertEquals(2, notifications.get());
    }
}
