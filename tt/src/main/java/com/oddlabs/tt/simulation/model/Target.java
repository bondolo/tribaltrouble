package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.core.net.Distributable;

public interface Target extends Distributable {
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
