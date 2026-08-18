package com.oddlabs.tt.procedural;

import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.LandscapeData;
import com.oddlabs.tt.simulation.model.Terrain;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Adapter exposing procedural landscape generation data to the simulation.
 *
 * @param config the configuration used to generate the island
 * @param landscape the procedural landscape instance
 */
public record GeneratedLandscapeData(@NonNull IslandConfig config,
                                     @NonNull Landscape landscape) implements LandscapeData {

    @Override
    public @NonNull Terrain terrain() {
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
    public float @NonNull [] heightmap() {
        return landscape.getHeight();
    }

    @Override
    public @NonNull List<int @NonNull []> trees() {
        return landscape.getTrees();
    }

    @Override
    public @NonNull List<int @NonNull []> palmTrees() {
        return landscape.getPalmtrees();
    }

    @Override
    public @NonNull List<int @NonNull []> rocks() {
        return landscape.getRock();
    }

    @Override
    public @NonNull List<int @NonNull []> iron() {
        return landscape.getIron();
    }

    @Override
    public float @NonNull [] @NonNull [] plants() {
        return landscape.getPlants();
    }

    @Override
    public boolean @NonNull [] @NonNull [] accessGrid() {
        return landscape.getAccessGrid();
    }

    @Override
    public byte @NonNull [] @NonNull [] buildGrid() {
        return landscape.getBuildGrid();
    }

    @Override
    public float @NonNull [] @NonNull [] startingLocations() {
        return landscape.getStartingLocations();
    }
}
