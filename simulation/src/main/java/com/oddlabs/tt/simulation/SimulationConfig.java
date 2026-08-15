package com.oddlabs.tt.simulation;

/**
 * Foundational simulation configuration constants and runtime defaults.
 */
public final class SimulationConfig {
    public static final float SEA_LEVEL = 0.1f;
    public static final boolean DEFAULT_RUN_AI = true;
    public static final int DEFAULT_GAME_SPEED = 2;
    public static final boolean DEFAULT_SLOW_MOTION = false;

    public static final boolean DEFAULT_PROCESS_LANDSCAPE = true;
    public static final boolean DEFAULT_PROCESS_TREES = true;
    public static final boolean DEFAULT_PROCESS_MISC = true;
    public static final boolean DEFAULT_PROCESS_SHADOWS = true;

    /**
     * Runtime debug flag enabling or disabling AI updates.
     */
    public static boolean run_ai = DEFAULT_RUN_AI;

    private SimulationConfig() {
    }
}
