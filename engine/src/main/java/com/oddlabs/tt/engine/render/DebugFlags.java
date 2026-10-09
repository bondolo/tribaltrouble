package com.oddlabs.tt.engine.render;

import java.util.logging.Logger;

/**
 * Interactive developer and debug flags for rendering subsystems.
 */
public final class DebugFlags {
    private static final Logger logger = Logger.getLogger(DebugFlags.class.getName());

    /** Displays on-screen engine status text and performance metrics. */
    public static boolean draw_status = false;

    /** Records detailed frame timing phases and GPU pass queries to a CSV log. */
    public static boolean frame_timing = Boolean.getBoolean("com.oddlabs.tt.frametiming");

    /** Enables visual mesh rasterization of terrain patches. */
    public static boolean draw_landscape = true;

    /** Enables rendering of trees and foliage sprites. */
    public static boolean draw_trees = true;

    /** Enables rendering of units, buildings, and misc dynamic scene elements. */
    public static boolean draw_misc = true;

    /** Enables rendering of particle systems, lightning, and transient visual effects. */
    public static boolean draw_particles = true;

    /** Enables water surface reflection, refraction, and wave rendering. */
    public static boolean draw_water = true;

    /** Enables sky dome and sea bottom background rendering. */
    public static boolean draw_sky = true;

    /** Draws Cartesian coordinate axes at the origin for spatial orientation. */
    public static boolean draw_axes = false;

    /** Enables high-detail multi-texturing and surface caustics on water and sea bottom. */
    public static boolean draw_detail = true;

    /** Enables rendering of shadow maps and projected shadow textures. */
    public static boolean draw_shadows = true;

    /** Applies scene lighting calculations to models and sprites. */
    public static boolean draw_light = true;

    /** Enables rendering of decorative ground plants and shrubbery. */
    public static boolean draw_plants = true;

    /** Renders diagnostic debug maps and overlays. */
    public static boolean draw_debug_maps = false;

    /** Enables multisample anti-aliasing (MSAA) for offscreen scene rendering. */
    public static boolean draw_msaa = true;

    /** Controls simulation processing, animation updates, and queueing for misc elements. */
    public static boolean process_misc = true;

    /** Controls visibility determination, LOD updates, and mesh preparation for landscape patches. */
    public static boolean process_landscape = true;

    /** Controls tree hierarchy traversal, animation updates, and LOD management. */
    public static boolean process_trees = true;

    /** Controls shadow generation, projection calculations, and receiver updates. */
    public static boolean process_shadows = true;

    /** Renders 3D geometry in wireframe mode. */
    public static boolean line_mode = false;

    /** Clears the color buffer before frame display (useful when diagnostic passes leave artifacts). */
    public static boolean clear_frame_buffer = false;

    /** Freezes view frustum culling to the current camera position for inspection. */
    public static boolean frustum_freeze = false;

    /** Drawing of debug bounding boxes. */
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
