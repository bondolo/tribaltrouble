package com.oddlabs.tt.engine.render;

import java.util.logging.Logger;

/**
 * Interactive developer and debug flags for rendering subsystems.
 */
public final class DebugFlags {
    private static final Logger logger = Logger.getLogger(DebugFlags.class.getName());

    public static boolean draw_status = false;
    public static boolean draw_landscape = true;
    public static boolean draw_trees = true;
    public static boolean draw_misc = true;
    public static boolean draw_particles = true;
    public static boolean draw_water = true;
    public static boolean draw_sky = true;
    public static boolean draw_axes = false;
    public static boolean draw_detail = true;
    public static boolean draw_shadows = true;
    public static boolean draw_light = true;
    public static boolean draw_plants = true;
    public static boolean draw_debug_maps = false;

    public static boolean process_misc = true;
    public static boolean process_landscape = true;
    public static boolean process_trees = true;
    public static boolean process_shadows = true;

    public static boolean line_mode = false;
    public static boolean clear_frame_buffer = false;
    public static boolean frustum_freeze = false;

    /**
     * Drawing of debug bounding boxes.
     */
    private static BoundingMode bounding = BoundingMode.NONE;

    public static void switchBoundingMode() {
        bounding = bounding.next();
        logger.info("Bounding mode: " + bounding);
    }

    public static boolean isBoundsEnabled(BoundingMode mode) {
        return bounding == mode || bounding == BoundingMode.ALL;
    }

    public static boolean debugRenderingEnabled() {
        return draw_axes || bounding != BoundingMode.NONE;
    }

    private DebugFlags() {
    }
}
