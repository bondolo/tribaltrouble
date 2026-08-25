package com.oddlabs.tt.simulation.landscape;


/**
 * Composite environment interface providing height querying, dimensional bounds, and visual client state.
 */
public interface LandscapeEnvironment {
    /**
     * Returns the nearest height at the specified world coordinate.
     *
     * @param x The X position in world units.
     * @param y The Y position in world units.
     * @return The elevation height at (x, y).
     */
    float getHeight(float x, float y);

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
