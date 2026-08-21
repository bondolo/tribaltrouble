package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.simulation.model.Terrain;

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
    float[] heightmap();

    /**
     * Returns the coordinates and attributes of standard trees.
     *
     * @return the standard trees list
     */
    List<int[]> trees();

    /**
     * Returns the coordinates and attributes of palm trees.
     *
     * @return the palm trees list
     */
    List<int[]> palmTrees();

    /**
     * Returns the coordinates of rock supply deposits.
     *
     * @return the rock deposits list
     */
    List<int[]> rocks();

    /**
     * Returns the coordinates of iron supply deposits.
     *
     * @return the iron deposits list
     */
    List<int[]> iron();

    /**
     * Returns the coordinates of cosmetic ground vegetation.
     *
     * @return the cosmetic plant coordinates
     */
    float[][] plants();

    /**
     * Returns the movement accessibility grid.
     *
     * @return the 2D accessibility boolean grid
     */
    boolean[][] accessGrid();

    /**
     * Returns the structure building placement grid.
     *
     * @return the 2D building placement byte grid
     */
    byte[][] buildGrid();

    /**
     * Returns the starting player coordinate pairs.
     *
     * @return the starting locations array
     */
    float[][] startingLocations();
}
