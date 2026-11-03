package com.oddlabs.tt.engine.render;

/**
 * Rendering engine configuration constants and defaults.
 */
public final class RenderConfig {
    public static final float FOV = 45.0f;
    public static final float VIEW_MIN = 0.1f;
    public static final float VIEW_MAX = 8000.0f;

    public static final int NO_MIPMAP_CUTOFF = 1000;

    public static final float MSAA_DPI_THRESHOLD_HIGH = 165.0f;
    public static final float MSAA_DPI_THRESHOLD_LOW = 130.0f;
    public static final int MSAA_MAX_HEIGHT_TIER1 = 1440;

    private RenderConfig() {
    }
}
