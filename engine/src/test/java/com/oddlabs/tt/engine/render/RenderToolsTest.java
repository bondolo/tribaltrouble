package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.base.geom.BoundingBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for frustum culling methods in {@link RenderTools}.
 */
final class RenderToolsTest {

    private static float[][] createCubeFrustum(float min, float max) {
        // 6 planes defining an axis-aligned box [min, max] where points inside have dot(plane, point) > 0:
        // Left plane (x > min):   +1*x + 0*y + 0*z - min > 0
        // Right plane (x < max):  -1*x + 0*y + 0*z + max > 0
        // Bottom plane (y > min):  0*x + 1*y + 0*z - min > 0
        // Top plane (y < max):     0*x - 1*y + 0*z + max > 0
        // Near plane (z > min):    0*x + 0*y + 1*z - min > 0
        // Far plane (z < max):     0*x + 0*y - 1*z + max > 0
        return new float[][]{
                {1f, 0f, 0f, -min},
                {-1f, 0f, 0f, max},
                {0f, 1f, 0f, -min},
                {0f, -1f, 0f, max},
                {0f, 0f, 1f, -min},
                {0f, 0f, -1f, max}
        };
    }

    @Test
    void testBoxCompletelyInside() {
        float[][] frustum = createCubeFrustum(-10f, 10f);
        BoundingBox box = new BoundingBox(-2f, 2f, -2f, 2f, -2f, 2f);

        int mask = RenderTools.testFrustum(box, frustum, RenderTools.ALL_PLANES_MASK);
        assertEquals(RenderTools.FRUSTUM_INSIDE, mask);
        assertEquals(RenderTools.FrustumIntersection.ALL_INSIDE, RenderTools.inFrustum(box, frustum));
    }

    @Test
    void testBoxCompletelyOutside() {
        float[][] frustum = createCubeFrustum(-10f, 10f);
        BoundingBox box = new BoundingBox(15f, 20f, -2f, 2f, -2f, 2f);

        int mask = RenderTools.testFrustum(box, frustum, RenderTools.ALL_PLANES_MASK);
        assertEquals(RenderTools.FRUSTUM_OUTSIDE, mask);
        assertEquals(RenderTools.FrustumIntersection.ALL_OUTSIDE, RenderTools.inFrustum(box, frustum));
    }

    @Test
    void testBoxIntersecting() {
        float[][] frustum = createCubeFrustum(-10f, 10f);
        // Straddles the right plane (x < 10): min x = 8, max x = 12
        BoundingBox box = new BoundingBox(8f, 12f, -2f, 2f, -2f, 2f);

        int mask = RenderTools.testFrustum(box, frustum, RenderTools.ALL_PLANES_MASK);
        assertTrue(mask > 0);
        // Plane 1 is the right plane (-1*x + 10)
        assertEquals(1 << 1, mask);
        assertEquals(RenderTools.FrustumIntersection.INTERSECTING, RenderTools.inFrustum(box, frustum));
    }

    @Test
    void testPlaneMaskInheritance() {
        float[][] frustum = createCubeFrustum(-10f, 10f);
        // Straddles plane 1 (x < 10)
        BoundingBox parent = new BoundingBox(8f, 12f, -2f, 2f, -2f, 2f);
        int parentMask = RenderTools.testFrustum(parent, frustum, RenderTools.ALL_PLANES_MASK);
        assertEquals(1 << 1, parentMask);

        // Child is fully inside plane 1 (min x = 8, max x = 9)
        BoundingBox childInside = new BoundingBox(8f, 9f, -1f, 1f, -1f, 1f);
        int childMask = RenderTools.testFrustum(childInside, frustum, parentMask);
        assertEquals(RenderTools.FRUSTUM_INSIDE, childMask);

        // Child is outside plane 1 (min x = 11, max x = 12)
        BoundingBox childOutside = new BoundingBox(11f, 12f, -1f, 1f, -1f, 1f);
        int childOutsideMask = RenderTools.testFrustum(childOutside, frustum, parentMask);
        assertEquals(RenderTools.FRUSTUM_OUTSIDE, childOutsideMask);
    }

    @Test
    void testFrustumInsideMaskShortcut() {
        float[][] frustum = createCubeFrustum(-10f, 10f);
        BoundingBox box = new BoundingBox(99f, 100f, 99f, 100f, 99f, 100f);

        // When planeMask is FRUSTUM_INSIDE (0), it must return FRUSTUM_INSIDE without testing planes
        int result = RenderTools.testFrustum(box, frustum, RenderTools.FRUSTUM_INSIDE);
        assertEquals(RenderTools.FRUSTUM_INSIDE, result);
    }
}
