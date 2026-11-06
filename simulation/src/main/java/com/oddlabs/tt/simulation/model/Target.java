package com.oddlabs.tt.simulation.model;

/**
 * Spatial or entity target that can be tracked, selected, or attacked.
 */
public interface Target {
    int getGridX();

    int getGridY();

    float getPositionX();

    float getPositionY();

    float getSize();

    boolean isDead();

    default boolean isAlive() {
        return !isDead();
    }
}
