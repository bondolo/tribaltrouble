package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.simulation.model.Terrain;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Spatial and topographical data required to construct and simulate the game world.
 */
public interface LandscapeData {

    /**
     * Returns the terrain type.
     *
     * @return the terrain type
     */
    @NonNull
    Terrain terrain();

    /**
     * Returns the dimensions of the world in meters.
     *
     * @return the dimensions of the world in meters
     */
    int metersPerWorld();

    /**
     * Returns the sea level altitude in meters.
     *
     * @return the sea level in meters
     */
    float seaLevelMeters();

    /**
     * Returns the terrain height elevation values.
     *
     * @return the height array
     */
    float @NonNull [] heightmap();

    /**
     * Returns the coordinates and attributes of standard trees.
     *
     * @return the standard trees list
     */
    @NonNull
    List<int @NonNull []> trees();

    /**
     * Returns the coordinates and attributes of palm trees.
     *
     * @return the palm trees list
     */
    @NonNull
    List<int @NonNull []> palmTrees();

    /**
     * Returns the coordinates of rock supply deposits.
     *
     * @return the rock deposits list
     */
    @NonNull
    List<int @NonNull []> rocks();

    /**
     * Returns the coordinates of iron supply deposits.
     *
     * @return the iron deposits list
     */
    @NonNull
    List<int @NonNull []> iron();

    /**
     * Returns the coordinates of cosmetic ground vegetation.
     *
     * @return the cosmetic plant coordinates
     */
    float @NonNull [] @NonNull [] plants();

    /**
     * Returns the movement accessibility grid.
     *
     * @return the 2D accessibility boolean grid
     */
    boolean @NonNull [] @NonNull [] accessGrid();

    /**
     * Returns the structure building placement grid.
     *
     * @return the 2D building placement byte grid
     */
    byte @NonNull [] @NonNull [] buildGrid();

    /**
     * Returns the starting player coordinate pairs.
     *
     * @return the starting locations array
     */
    float @NonNull [] @NonNull [] startingLocations();
}
