package com.oddlabs.tt.procedural.landscape;

import com.oddlabs.tt.base.util.ProgressListener;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;

import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * Generates landscape heights and topography for game islands.
 */
public final class IslandGenerator implements WorldGenerator<Landscape> {
    private static final Logger logger = Logger.getLogger(IslandGenerator.class.getSimpleName());
    @Serial
    private static final long serialVersionUID = 1;

    private final IslandConfig config;

    public IslandGenerator(IslandConfig config) {
        this.config = config;
    }

    public IslandConfig getConfig() {
        return config;
    }

    @Override
    public int getMetersPerWorld() {
        return config.metersPerWorld();
    }

    @Override
    public Landscape generate(int num_players, int initial_unit_count, float random_start_pos) {
        // Build landscape
        Instant time_before = Instant.now();
        ProceduralLandscape landscape = new ProceduralLandscape(num_players, config, initial_unit_count, random_start_pos);
        Instant time_after = Instant.now();
        logger.fine(() -> "Landscape created in " + Duration.between(time_before, time_after));

        ProgressListener.progress();
        return landscape;
    }
}
