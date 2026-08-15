package com.oddlabs.tt;

import com.oddlabs.tt.engine.render.BoundingMode;
import com.oddlabs.tt.simulation.SimulationConfig;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.file.Path;

/**
 * Global constant definitions and utility configurations.
 */
public final class Globals {
    public static final int DETAIL_LOW = 0;
    public static final int DETAIL_NORMAL = 1;
    public static final int DETAIL_HIGH = 2;

    public static final int[] TEXTURE_MIP_SHIFT = new int[]{1, 0, 0};
    public static final int[] UNIT_HIGH_POLY_COUNT = new int[]{20000, 80000, 200000};
    public static final int[] LANDSCAPE_POLY_COUNT = new int[]{10000, 40000, 100000};
    public static final boolean[] INSERT_PLANTS = new boolean[]{false, true, true};

    public static final String GAME_NAME = "TribalTrouble";
    public static final int REVISION = 1;
    public static final Path SETTINGS_FILE_NAME = Path.of("settings");

    public static boolean run_ai = SimulationConfig.DEFAULT_RUN_AI;

    public static int gamespeed = SimulationConfig.DEFAULT_GAME_SPEED;

    public static final boolean process_landscape = SimulationConfig.DEFAULT_PROCESS_LANDSCAPE;
    public static final boolean process_trees = SimulationConfig.DEFAULT_PROCESS_TREES;
    public static boolean process_misc = SimulationConfig.DEFAULT_PROCESS_MISC;
    public static final boolean process_shadows = SimulationConfig.DEFAULT_PROCESS_SHADOWS;

    public static boolean draw_status = false;
    public static final boolean draw_landscape = true;
    public static final boolean draw_trees = true;
    public static boolean draw_misc = true;
    public static boolean draw_particles = true;
    public static boolean draw_water = true;
    public static final boolean draw_sky = true;
    public static boolean draw_axes = false;
    public static boolean draw_detail = true;
    public static boolean draw_shadows = true;
    public static boolean draw_light = true;
    public static boolean draw_plants = true;
    public static boolean draw_debug_maps = false;

    public static final boolean line_mode = false;
    public static boolean clear_frame_buffer = false;
    public static boolean frustum_freeze = false;

    public static boolean slowmotion = SimulationConfig.DEFAULT_SLOW_MOTION;

    public static boolean checksum_error_in_last_game = false;

    /**
     * Drawing of debug bounding boxes.
     */
    private static @NonNull BoundingMode bounding = BoundingMode.NONE;

    public static void switchBoundingMode() {
        bounding = bounding.next();
        IO.println("Bounding mode: " + bounding);
    }

    public static boolean isBoundsEnabled(@NonNull BoundingMode mode) {
        return bounding == mode || bounding == BoundingMode.ALL;
    }

    public static boolean debugRenderingEnabled() {
        return draw_axes || bounding != BoundingMode.NONE;
    }

    public static final int COMPRESSED_RGB_FORMAT = GL13.GL_COMPRESSED_RGB;
    public static final int COMPRESSED_RGBA_FORMAT = GL13.GL_COMPRESSED_RGBA;
    public static final int COMPRESSED_A_FORMAT = 0x8229; // GL_R8
    public static final int COMPRESSED_LUMINANCE_FORMAT = GL11.GL_RED;
    public static int LOW_DETAIL_TEXTURE_SHIFT = 1;

    public static final float LANDSCAPE_HILLS = 1f;
    public static final float LANDSCAPE_VEGETATION = 2f;
    public static final float LANDSCAPE_RESOURCES = 0f;
    public static final int LANDSCAPE_SEED = 1;

    public static final float LANDSCAPE_TEXTURE_SCALE = 1.0f / 16.0f;

    public static final int VIEW_BIT_DEPTH = 16;
    public static final float FOV = 45.0f;
    public static final float VIEW_MIN = 0.1f;
    public static final float VIEW_MAX = 8000.0f;

    public static final int NET_PORT = 21000;

    public static final int NO_MIPMAP_CUTOFF = 1000;

    public static final int STRUCTURE_SIZE = 256;
    public static final int DETAIL_SIZE = 256;
    public static final int TEXELS_PER_GRID_UNIT = 8;

    public static final float LANDSCAPE_DETAIL_REPEAT_RATE = 0.25f;
    public static final float WATER_REPEAT_RATE = 0.001f;
    public static final float WATER_DETAIL_REPEAT_RATE = 0.01f;
    public static final int LANDSCAPE_DETAIL_FADEOUT_BASE_LEVEL = 2;
    public static final float LANDSCAPE_DETAIL_FADEOUT_FACTOR = 0.75f;

    public static final int MAX_RENDERNODE_DEPTH = 5;

    public static final String SCREENSHOT_DEFAULT = "screenshot";

    public static final float TREE_ERROR_DISTANCE = 100f;

    public static final float WHEEL_SCALE = 0.01f;

    public static final int CURSOR_BLINK_TIME = 1000;

    public static final int FPS_WIDTH = 800;

    public static final int SHELL_HISTORY_SIZE = 50;
    public static final int SHELL_HISTORY_PAGE_SIZE = 10;


    public static final float SEA_LEVEL = SimulationConfig.SEA_LEVEL;
    public static final int TEXELS_PER_CHUNK_BORDER = 4;

    public static final int BLOCK_SCROLL_AMOUNT = 20;

    public static final float ERROR_TOLERANCE = 10f;

    private Globals() {
    }
}
