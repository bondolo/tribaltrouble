package com.oddlabs.tt.simulation.model;

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
