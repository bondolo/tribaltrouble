package com.oddlabs.tt.base.geom;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BoundingBox}.
 */
final class BoundingBoxTest {

    private static FrustumIntersection createCubeFrustum(float min, float max) {
        Matrix4f ortho = new Matrix4f().ortho(min, max, min, max, -max, -min);
        return new FrustumIntersection(ortho);
    }

    @Test
    void testBoxCompletelyInside() {
        FrustumIntersection frustum = createCubeFrustum(-10f, 10f);
        BoundingBox box = new BoundingBox(-2f, 2f, -2f, 2f, -2f, 2f);

        int result = box.intersectFrustum(frustum);
        assertEquals(FrustumIntersection.INSIDE, result);
        assertTrue(result < 0);
        assertTrue(box.testFrustum(frustum));
    }

    @Test
    void testBoxCompletelyOutside() {
        FrustumIntersection frustum = createCubeFrustum(-10f, 10f);
        BoundingBox box = new BoundingBox(15f, 20f, -2f, 2f, -2f, 2f);

        int result = box.intersectFrustum(frustum);
        assertTrue(result >= 0);
        assertFalse(box.testFrustum(frustum));
    }

    @Test
    void testBoxIntersecting() {
        FrustumIntersection frustum = createCubeFrustum(-10f, 10f);
        // Straddles the right plane (x < 10): min x = 8, max x = 12
        BoundingBox box = new BoundingBox(8f, 12f, -2f, 2f, -2f, 2f);

        int result = box.intersectFrustum(frustum);
        assertEquals(FrustumIntersection.INTERSECT, result);
        assertTrue(result < 0);
        assertTrue(box.testFrustum(frustum));
    }
}
