package com.oddlabs.tt.procedural;

/**
 * Configuration constants for procedural landscape and terrain generation.
 */
public final class LandscapeConfig {
    public static final int STRUCTURE_SIZE = 256;
    public static final int DETAIL_SIZE = 256;
    public static final int TEXELS_PER_GRID_UNIT = 8;
    public static final int TEXELS_PER_CHUNK_BORDER = 4;

    public static final float LANDSCAPE_HILLS = 1.0f;
    public static final float LANDSCAPE_VEGETATION = 2.0f;
    public static final float LANDSCAPE_RESOURCES = 0.0f;
    public static final int LANDSCAPE_SEED = 1;

    public static final float LANDSCAPE_TEXTURE_SCALE = 1.0f / 16.0f;
    public static final float LANDSCAPE_DETAIL_REPEAT_RATE = 0.25f;
    public static final float WATER_REPEAT_RATE = 0.001f;
    public static final float WATER_DETAIL_REPEAT_RATE = 0.01f;
    public static final int LANDSCAPE_DETAIL_FADEOUT_BASE_LEVEL = 2;
    public static final float LANDSCAPE_DETAIL_FADEOUT_FACTOR = 0.75f;

    private LandscapeConfig() {
    }
}
