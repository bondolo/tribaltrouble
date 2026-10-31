package com.oddlabs.tt.base.animation;

/**
 * Interface providing deterministic simulation tick clock and game timer queries.
 */
public interface SimulationClock {
    int GAMESPEED_DONTCARE = -2;

    /**
     * {@return the current simulation tick number}
     */
    int getTick();

    /**
     * {@return seconds per simulation tick}
     */
    float getSecondsPerTick();

    /**
     * {@return sub-tick presentation interpolation factor alpha in [0, 1]}
     */
    default float getInterpolationAlpha() {
        return 1.0f;
    }
}
