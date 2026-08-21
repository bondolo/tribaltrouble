package com.oddlabs.tt.procedural;

import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.LandscapeData;
import com.oddlabs.tt.simulation.model.Terrain;

import java.util.List;

/**
 * Adapter exposing procedural landscape generation data to the simulation.
 *
 * @param config the configuration used to generate the island
 * @param landscape the procedural landscape instance
 */
public record GeneratedLandscapeData(IslandConfig config,
                                     Landscape landscape) implements LandscapeData {

    @Override
    public Terrain terrain() {
        return config.terrain();
    }

    @Override
    public int metersPerWorld() {
        return config.metersPerWorld();
    }

    @Override
    public float seaLevelMeters() {
        return landscape.getSeaLevelMeters();
    }

    @Override
    public float[] heightmap() {
        return landscape.getHeight();
    }

    @Override
    public List<int[]> trees() {
        return landscape.getTrees();
    }

    @Override
    public List<int[]> palmTrees() {
        return landscape.getPalmtrees();
    }

    @Override
    public List<int[]> rocks() {
        return landscape.getRock();
    }

    @Override
    public List<int[]> iron() {
        return landscape.getIron();
    }

    @Override
    public float[][] plants() {
        return landscape.getPlants();
    }

    @Override
    public boolean[][] accessGrid() {
        return landscape.getAccessGrid();
    }

    @Override
    public byte[][] buildGrid() {
        return landscape.getBuildGrid();
    }

    @Override
    public float[][] startingLocations() {
        return landscape.getStartingLocations();
    }
}
