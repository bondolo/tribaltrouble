package com.oddlabs.tt.simulation.pathfinder;

import org.jspecify.annotations.Nullable;

public interface ScanFilter {
    int getMinRadius();

    int getMaxRadius();

    /**
     * Scan and filter using the provided occupant
     *
     * @return true if scan should stop otherwise false to continue scan
     */
    boolean filter(int grid_x, int grid_y, @Nullable Occupant occ);

    /**
     * {@return true if this filter only matches non-null occupants, allowing empty tiles and chunks to be skipped}
     */
    default boolean requiresOccupant() {
        return false;
    }
}
