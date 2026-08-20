package com.oddlabs.tt.engine.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Rendering engine configuration constants and defaults.
 */
public final class RenderConfig {
    public static final float FOV = 45.0f;
    public static final float VIEW_MIN = 0.1f;
    public static final float VIEW_MAX = 8000.0f;
    public static final int VIEW_BIT_DEPTH = 16;

    public static final int NO_MIPMAP_CUTOFF = 1000;
    public static final int MAX_RENDERNODE_DEPTH = 5;
    public static final int FPS_WIDTH = 800;
    public static final String SCREENSHOT_DEFAULT = "screenshot";

    public static final int COMPRESSED_RGB_FORMAT = GL13.GL_COMPRESSED_RGB;
    public static final int COMPRESSED_RGBA_FORMAT = GL13.GL_COMPRESSED_RGBA;
    public static final int COMPRESSED_A_FORMAT = 0x8229; // GL_R8
    public static final int COMPRESSED_LUMINANCE_FORMAT = GL11.GL_RED;

    public static final int[] TEXTURE_MIP_SHIFT = new int[]{1, 0, 0};
    public static final int[] UNIT_HIGH_POLY_COUNT = new int[]{20000, 80000, 200000};
    public static final int[] LANDSCAPE_POLY_COUNT = new int[]{10000, 40000, 100000};
    public static final boolean[] INSERT_PLANTS = new boolean[]{false, true, true};
    public static final int LOW_DETAIL_TEXTURE_SHIFT = 1;

    public static final float TREE_ERROR_DISTANCE = 100f;
    public static final float ERROR_TOLERANCE = 10f;

    public static final boolean DEFAULT_PROCESS_LANDSCAPE = true;
    public static final boolean DEFAULT_PROCESS_TREES = true;
    public static final boolean DEFAULT_PROCESS_MISC = true;
    public static final boolean DEFAULT_PROCESS_SHADOWS = true;

    private RenderConfig() {
    }
}
