package com.oddlabs.tt.procedural.landscape;

import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.LandscapeData;
import com.oddlabs.tt.simulation.landscape.PlantLocation;
import com.oddlabs.tt.simulation.landscape.SpawnLocation;
import com.oddlabs.tt.simulation.landscape.SupplyPlacement;
import com.oddlabs.tt.simulation.landscape.TreePlacement;
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
    public List<TreePlacement> trees() {
        return landscape.getTrees();
    }

    @Override
    public List<TreePlacement> palmTrees() {
        return landscape.getPalmtrees();
    }

    @Override
    public List<SupplyPlacement> rocks() {
        return landscape.getRock();
    }

    @Override
    public List<SupplyPlacement> iron() {
        return landscape.getIron();
    }

    @Override
    public List<PlantLocation>[] plants() {
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
    public List<List<SpawnLocation>> startingLocations() {
        return landscape.getStartingLocations();
    }
}
