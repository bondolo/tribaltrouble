package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.engine.util.DebugRender;

/**
 * Utility class providing common rendering operations, including frustum culling,
 * coordinate transformations, and debug visualization.
 */
public final class RenderTools {

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

    public static FrustumIntersection inFrustum(BoundingBox box, float[][] frustum) {
        boolean all_corners_in_all_planes = true;

        for (int f = 0; f < 6; f++) {
            float planeA = frustum[f][0];
            float planeB = frustum[f][1];
            float planeC = frustum[f][2];
            float planeD = frustum[f][3];

            // P-vertex: corner with the maximum signed distance along the plane normal
            float px = planeA >= 0 ? box.bmax_x : box.bmin_x;
            float py = planeB >= 0 ? box.bmax_y : box.bmin_y;
            float pz = planeC >= 0 ? box.bmax_z : box.bmin_z;

            if (planeA * px + planeB * py + planeC * pz + planeD <= 0) {
                return FrustumIntersection.ALL_OUTSIDE;
            }

            // N-vertex: corner with the minimum signed distance along the plane normal
            if (all_corners_in_all_planes) {
                float nx = planeA >= 0 ? box.bmin_x : box.bmax_x;
                float ny = planeB >= 0 ? box.bmin_y : box.bmax_y;
                float nz = planeC >= 0 ? box.bmin_z : box.bmax_z;

                if (planeA * nx + planeB * ny + planeC * nz + planeD <= 0) {
                    all_corners_in_all_planes = false;
                }
            }
        }

        return all_corners_in_all_planes ? FrustumIntersection.ALL_INSIDE : FrustumIntersection.INTERSECTING;
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
