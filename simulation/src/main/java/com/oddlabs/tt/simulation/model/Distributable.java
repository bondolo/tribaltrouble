package com.oddlabs.tt.simulation.model;

/**
 * Marker interface for simulation objects that can be identified and distributed over network events.
 */
public interface Distributable {
    int NO_ID = 0;

    default int getId() {
        return NO_ID;
    }

    default void setId(int id) {
    }
}
