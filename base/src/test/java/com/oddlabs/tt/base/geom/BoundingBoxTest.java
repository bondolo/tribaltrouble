package com.oddlabs.tt.base.geom;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
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

    @Test
    void testCopyConstructor() {
        BoundingBox original = new BoundingBox(1f, 5f, 2f, 6f, 3f, 7f);
        BoundingBox copy = new BoundingBox(original);

        assertEquals(original.bmin_x, copy.bmin_x);
        assertEquals(original.bmax_x, copy.bmax_x);
        assertEquals(original.bmin_y, copy.bmin_y);
        assertEquals(original.bmax_y, copy.bmax_y);
        assertEquals(original.bmin_z, copy.bmin_z);
        assertEquals(original.bmax_z, copy.bmax_z);
        assertEquals(original.getCX(), copy.getCX());
        assertEquals(original.getCY(), copy.getCY());
        assertEquals(original.getCZ(), copy.getCZ());
    }

    @Test
    void testExpandSinglePointInitializesBothMinAndMax() {
        BoundingBox box = new BoundingBox();
        box.expandBoundsX(3f);
        box.expandBoundsY(-4f);
        box.expandBoundsZ(5f);

        assertTrue(box.isValid());
        assertEquals(3f, box.bmin_x);
        assertEquals(3f, box.bmax_x);
        assertEquals(-4f, box.bmin_y);
        assertEquals(-4f, box.bmax_y);
        assertEquals(5f, box.bmin_z);
        assertEquals(5f, box.bmax_z);
        assertEquals(3f, box.getCX());
        assertEquals(-4f, box.getCY());
        assertEquals(5f, box.getCZ());
    }

    @Test
    void testContainsIncludesBoundaryEquality() {
        BoundingBox outer = new BoundingBox(0f, 10f, 0f, 10f, -2f, 5f);
        BoundingBox touchingMin = new BoundingBox(0f, 5f, 0f, 5f, -2f, 3f);

        assertTrue(outer.contains(touchingMin));
        assertTrue(outer.contains(outer));
    }

    @Test
    void testIntersectsRay() {
        BoundingBox box = new BoundingBox(-1f, 1f, -1f, 1f, -1f, 1f);
        Vector2f nearFar = new Vector2f();

        assertTrue(box.intersectsRay(0f, 0f, 5f, 0f, 0f, -1f, nearFar));
        assertEquals(4f, nearFar.x, 1e-5f);
        assertEquals(6f, nearFar.y, 1e-5f);
        assertFalse(box.intersectsRay(5f, 5f, 5f, 0f, 0f, -1f, nearFar));
    }

    @Test
    void testSetTransformedBounds() {
        BoundingBox source = new BoundingBox(-1.5f, 2.5f, -3.0f, 4.0f, 0.5f, 5.5f);
        Matrix4f matrix = new Matrix4f()
                .translation(10f, 20f, -5f)
                .rotate((float) Math.toRadians(35.0), 1f, 0f, 0f)
                .rotate((float) Math.toRadians(-45.0), 0f, 1f, 0f)
                .rotate((float) Math.toRadians(20.0), 0f, 0f, 1f)
                .scale(1.2f, 0.8f, 1.5f);

        Vector4f v = new Vector4f();
        Vector4f dest = new Vector4f();
        float expectedMinX = Float.POSITIVE_INFINITY;
        float expectedMaxX = Float.NEGATIVE_INFINITY;
        float expectedMinY = Float.POSITIVE_INFINITY;
        float expectedMaxY = Float.NEGATIVE_INFINITY;
        float expectedMinZ = Float.POSITIVE_INFINITY;
        float expectedMaxZ = Float.NEGATIVE_INFINITY;
        for (float x : new float[]{source.bmin_x, source.bmax_x}) {
            for (float y : new float[]{source.bmin_y, source.bmax_y}) {
                for (float z : new float[]{source.bmin_z, source.bmax_z}) {
                    v.set(x, y, z, 1f);
                    matrix.transform(v, dest);
                    if (dest.x < expectedMinX) expectedMinX = dest.x;
                    if (dest.x > expectedMaxX) expectedMaxX = dest.x;
                    if (dest.y < expectedMinY) expectedMinY = dest.y;
                    if (dest.y > expectedMaxY) expectedMaxY = dest.y;
                    if (dest.z < expectedMinZ) expectedMinZ = dest.z;
                    if (dest.z > expectedMaxZ) expectedMaxZ = dest.z;
                }
            }
        }

        BoundingBox transformed = new BoundingBox();
        transformed.setTransformedBounds(source, matrix);

        assertEquals(expectedMinX, transformed.bmin_x, 1e-5f);
        assertEquals(expectedMaxX, transformed.bmax_x, 1e-5f);
        assertEquals(expectedMinY, transformed.bmin_y, 1e-5f);
        assertEquals(expectedMaxY, transformed.bmax_y, 1e-5f);
        assertEquals(expectedMinZ, transformed.bmin_z, 1e-5f);
        assertEquals(expectedMaxZ, transformed.bmax_z, 1e-5f);
        assertEquals((expectedMinX + expectedMaxX) * 0.5f, transformed.getCX(), 1e-5f);
        assertEquals((expectedMinY + expectedMaxY) * 0.5f, transformed.getCY(), 1e-5f);
        assertEquals((expectedMinZ + expectedMaxZ) * 0.5f, transformed.getCZ(), 1e-5f);
    }
}
