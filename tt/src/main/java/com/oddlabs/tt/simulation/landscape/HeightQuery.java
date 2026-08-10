package com.oddlabs.tt.simulation.landscape;

/**
 * Functional interface for querying elevation heights on the landscape grid.
 */
@FunctionalInterface
public interface HeightQuery {
    /**
     * Returns the nearest height at the specified world coordinate.
     *
     * @param x The X position in world units.
     * @param y The Y position in world units.
     * @return The elevation height at (x, y).
     */
    float getHeight(float x, float y);
}
