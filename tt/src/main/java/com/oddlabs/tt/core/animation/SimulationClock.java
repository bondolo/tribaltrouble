package com.oddlabs.tt.core.animation;

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
}
