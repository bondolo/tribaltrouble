package com.oddlabs.tt.procedural.landscape;

import org.joml.Vector3f;
import org.joml.Vector3fc;

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

    public static final float LANDSCAPE_TEXTURE_SCALE = 1.0f / 64.0f;
    public static final float LANDSCAPE_DETAIL_REPEAT_RATE = 0.25f;
    public static final float WATER_REPEAT_RATE = 0.001f;
    public static final float WATER_DETAIL_REPEAT_RATE = 0.01f;
    public static final int LANDSCAPE_DETAIL_FADEOUT_BASE_LEVEL = 2;
    public static final float LANDSCAPE_DETAIL_FADEOUT_FACTOR = 0.75f;

    /**
     * Solar elevation angle in degrees (near-noon 70 degrees for 360-degree multiplayer symmetry).
     */
    public static final float SUN_ELEVATION_DEGREES = 70.0f;

    /**
     * Solar azimuth angle in degrees (225 degrees, Southwest).
     */
    public static final float SUN_AZIMUTH_DEGREES = 225.0f;

    /**
     * Canonical light direction X component pointing toward the sun, derived from solar angles.
     */
    public static final float LIGHT_DIR_X = (float) (Math.cos(Math.toRadians(SUN_ELEVATION_DEGREES)) * Math.cos(Math
            .toRadians(SUN_AZIMUTH_DEGREES)));

    /**
     * Canonical light direction Y component pointing toward the sun, derived from solar angles.
     */
    public static final float LIGHT_DIR_Y = (float) (Math.cos(Math.toRadians(SUN_ELEVATION_DEGREES)) * Math.sin(Math
            .toRadians(SUN_AZIMUTH_DEGREES)));

    /**
     * Canonical light direction Z component pointing toward the sun, derived from solar angles.
     */
    public static final float LIGHT_DIR_Z = (float) Math.sin(Math.toRadians(SUN_ELEVATION_DEGREES));

    /**
     * Canonical light direction vector pointing toward the sun.
     */
    public static final Vector3fc LIGHT_DIR = new Vector3f(LIGHT_DIR_X, LIGHT_DIR_Y, LIGHT_DIR_Z);

    private LandscapeConfig() {
    }
}
