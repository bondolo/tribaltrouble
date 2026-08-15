package com.oddlabs.tt.simulation.landscape;

/**
 * Provides basic dimensional and environmental boundary information for a landscape.
 */
public interface LandscapeBounds {
    /**
     * {@return the total size of the world in meters/world units along each axis}
     */
    int getMetersPerWorld();

    /**
     * {@return the sea level height in meters}
     */
    float getSeaLevelMeters();

    /**
     * {@return the total number of terrain patches per world side}
     */
    int getPatchesPerWorld();

    /**
     * {@return the size of a single terrain patch in meters}
     */
    int getMetersPerPatch();

    /**
     * {@return the number of grid units per world side}
     */
    int getGridUnitsPerWorld();

    /**
     * {@return true if the specified patch coordinate is below sea level}
     */
    boolean isBelowSeaLevel(int patchX, int patchY);
}
