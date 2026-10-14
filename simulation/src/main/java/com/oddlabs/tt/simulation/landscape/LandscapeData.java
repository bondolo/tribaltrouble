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
     * Returns the grid coordinates of standard tree placements.
     *
     * @return the standard tree placements list
     */
    List<TreePlacement> trees();

    /**
     * Returns the grid coordinates of palm tree placements.
     *
     * @return the palm tree placements list
     */
    List<TreePlacement> palmTrees();

    /**
     * Returns the grid coordinates of rock supply deposits.
     *
     * @return the rock supply placements list
     */
    List<SupplyPlacement> rocks();

    /**
     * Returns the grid coordinates of iron supply deposits.
     *
     * @return the iron supply placements list
     */
    List<SupplyPlacement> iron();

    /**
     * Returns the world coordinates of cosmetic ground vegetation, indexed by plant type.
     *
     * @return the plant placements indexed by type, then by placement
     */
    List<PlantLocation>[] plants();

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
     * Returns the per-player spawn locations for initial units.
     *
     * @return the starting locations list indexed by player, then by unit
     */
    List<List<SpawnLocation>> startingLocations();
}
