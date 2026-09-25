package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.engine.util.DebugRender;

/**
 * Utility class providing common rendering operations, including frustum culling,
 * coordinate transformations, and debug visualization.
 */
public final class RenderTools {

    public static final int ALL_PLANES_MASK = 0x3F;
    public static final int FRUSTUM_OUTSIDE = -1;
    public static final int FRUSTUM_INSIDE = 0;

    public enum FrustumIntersection {
        ALL_OUTSIDE,
        INTERSECTING,
        ALL_INSIDE
    }

    /**
     * Translates and rotates the matrix stack to position and orient a model.
     * The rotation is calculated from the direction vector (dir_x, dir_y).
     */
    static void translateAndRotate(Model model, MatrixStack stack) {
        translateAndRotate(model.getPositionX(), model.getPositionY(), model.getPositionZ(), model.getDirectionX(),
                model.getDirectionY(), stack);
    }

    /**
     * Translates and rotates the matrix stack to position and orient a model.
     * The rotation is calculated from the direction vector (dir_x, dir_y).
     */
    static void translateAndRotate(float x, float y, float z, float dir_x, float dir_y, MatrixStack stack) {
        float angle = (float) Math.toDegrees(Math.atan2(dir_y, dir_x));
        stack.translate(x, y, z).rotate(angle, 0f, 0f, 1f);
    }

    /**
     * Tests a bounding box against a subset of frustum planes specified by a bitmask.
     *
     * @param box the bounding box to test
     * @param frustum 6-plane frustum array [6][4]
     * @param planeMask bitmask of active planes to test (bits 0 to 5)
     * @return {@link #FRUSTUM_OUTSIDE} (-1) if box is completely outside any tested plane;
     *         {@link #FRUSTUM_INSIDE} (0) if box is completely inside all tested planes;
     *         otherwise a non-zero bitmask containing only the planes that clip this box.
     */
    public static int testFrustum(BoundingBox box, float[][] frustum, int planeMask) {
        if (planeMask == FRUSTUM_INSIDE) {
            return FRUSTUM_INSIDE;
        }
        int nextMask = 0;
        int activePlanes = planeMask;
        while (activePlanes != 0) {
            int f = Integer.numberOfTrailingZeros(activePlanes);
            int planeBit = 1 << f;
            activePlanes &= ~planeBit;

            float[] plane = frustum[f];
            float planeA = plane[0];
            float planeB = plane[1];
            float planeC = plane[2];
            float planeD = plane[3];

            // P-vertex: corner with maximum signed distance along plane normal
            float px = planeA >= 0f ? box.bmax_x : box.bmin_x;
            float py = planeB >= 0f ? box.bmax_y : box.bmin_y;
            float pz = planeC >= 0f ? box.bmax_z : box.bmin_z;

            if (planeA * px + planeB * py + planeC * pz + planeD <= 0f) {
                return FRUSTUM_OUTSIDE;
            }

            // N-vertex: corner with minimum signed distance along plane normal
            float nx = planeA >= 0f ? box.bmin_x : box.bmax_x;
            float ny = planeB >= 0f ? box.bmin_y : box.bmax_y;
            float nz = planeC >= 0f ? box.bmin_z : box.bmax_z;

            if (planeA * nx + planeB * ny + planeC * nz + planeD <= 0f) {
                nextMask |= planeBit;
            }
        }
        return nextMask;
    }

    public static FrustumIntersection inFrustum(BoundingBox box, float[][] frustum) {
        int mask = testFrustum(box, frustum, ALL_PLANES_MASK);
        if (mask == FRUSTUM_OUTSIDE) {
            return FrustumIntersection.ALL_OUTSIDE;
        }
        return mask == FRUSTUM_INSIDE ? FrustumIntersection.ALL_INSIDE : FrustumIntersection.INTERSECTING;
    }

    public static float getEyeDistanceSquared(BoundingBox box, float camera_x, float camera_y,
            float camera_z) {
        float distx = camera_x - box.getCX();
        float disty = camera_y - box.getCY();
        float distz = camera_z - box.getCZ();
        return distx * distx + disty * disty + distz * distz;
    }

    public static float getCameraDistanceXYSquared(BoundingBox box, float camera_x, float camera_y) {
        float dx = camera_x - box.getCX();
        float dy = camera_y - box.getCY();
        return dx * dx + dy * dy;
    }

    public static float getCameraDistanceSquared(BoundingBox box, float camera_x, float camera_y,
            float camera_z) {
        return getEyeDistanceSquared(box, camera_x, camera_y, camera_z);
    }

    public static void draw(BoundingBox box) {
        draw(box, 1f, 1f, 1f);
    }

    public static void draw(BoundingBox box, float r, float g, float b) {
        DebugRender.drawBox(box.bmin_x, box.bmax_x, box.bmin_y, box.bmax_y, box.bmin_z, box.bmax_z, r, g, b);
    }

    public static void draw(BoundingBox box, BoundingMode bound_type, float r, float g, float b) {
        draw(box, r, g, b);
    }

    private RenderTools() {
        // no instances
    }
}
