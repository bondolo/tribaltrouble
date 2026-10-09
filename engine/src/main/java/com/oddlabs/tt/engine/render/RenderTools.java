package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.engine.util.DebugRender;

/**
 * Utility class providing common rendering operations, including coordinate transformations,
 * distance calculations, and debug visualization.
 */
public final class RenderTools {

    /**
     * Translates and rotates the matrix stack to position and orient a model.
     * The rotation is calculated from the direction vector (dir_x, dir_y).
     */
    static void translateAndRotate(float x, float y, float z, float dir_x, float dir_y, MatrixStack stack) {
        float angle = (float) Math.toDegrees(Math.atan2(dir_y, dir_x));
        stack.translate(x, y, z).rotate(angle, 0f, 0f, 1f);
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
