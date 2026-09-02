package com.oddlabs.tt.engine.render;

/**
 * Rendering engine configuration constants and defaults.
 */
public final class RenderConfig {
    public static final float FOV = 45.0f;
    public static final float VIEW_MIN = 0.1f;
    public static final float VIEW_MAX = 8000.0f;

    public static final int NO_MIPMAP_CUTOFF = 1000;

    public static final int[] UNIT_HIGH_POLY_COUNT = new int[]{20000, 80000, 200000};
    public static final int[] LANDSCAPE_POLY_COUNT = new int[]{10000, 40000, 100000};
    public static final boolean[] INSERT_PLANTS = new boolean[]{false, true, true};

    private RenderConfig() {
    }
}
