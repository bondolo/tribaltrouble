package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.simulation.model.Terrain;

import java.util.List;

/**
 * Spatial and topographical landscape defining the terrain and object placements of the world.
 */
public interface Landscape {

    /**
     * Returns the configuration used to generate the island.
     *
     * @return the island configuration
     */
    IslandConfig config();

    /**
     * Returns the terrain type.
     *
     * @return the terrain type
     */
    default Terrain terrain() {
        return config().terrain();
    }

    /**
     * Returns the dimensions of the world in meters.
     *
     * @return the dimensions of the world in meters
     */
    default int metersPerWorld() {
        return config().metersPerWorld();
    }

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

    /**
     * Returns the vertical height scaling factor applied to the heightmap.
     *
     * @return the vertical height scale
     */
    default int heightScale() {
        return switch (metersPerWorld()) {
            case 256 -> 32;
            case 512 -> 48;
            case 1024 -> 64;
            default -> 32;
        };
    }

    /**
     * Returns the threshold value for unit accessibility on slopes.
     *
     * @return the slope access threshold
     */
    default float accessThreshold() {
        return switch (metersPerWorld()) {
            case 256 -> 0.05f;
            case 512 -> 0.0375f;
            case 1024 -> 0.025f;
            default -> 0.05f;
        };
    }

    /**
     * Returns the threshold value for building placement on slopes.
     *
     * @return the slope build threshold
     */
    default float buildThreshold() {
        return accessThreshold() / 2f;
    }
}
