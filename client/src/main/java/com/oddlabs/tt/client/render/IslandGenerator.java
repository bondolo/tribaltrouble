package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.base.util.ProgressListener;
import com.oddlabs.tt.procedural.GeneratedLandscapeData;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.procedural.LandscapeConfig;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;

import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * Generates landscape heights and topography for game islands.
 */
public final class IslandGenerator implements WorldGenerator<GeneratedLandscapeData> {
    private static final Logger logger = Logger.getLogger(IslandGenerator.class.getSimpleName());
    @Serial
    private static final long serialVersionUID = 1;

    private static final int IDEAL_TEXELS_PER_DETAIL = 256;
    private static final float IDEAL_DETAIL_ALPHA = .15f;

    private final IslandConfig config;

    public IslandGenerator(IslandConfig config) {
        this(config, AppConfig.DEFAULT_TEXELS_PER_GRID_UNIT);
    }

    public IslandGenerator(IslandConfig config, int texels_per_grid_unit) {
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
    public GeneratedLandscapeData generate(int num_players, int initial_unit_count, float random_start_pos) {
        // Build landscape
        Instant time_before = Instant.now();
        int base_level = LandscapeConfig.LANDSCAPE_DETAIL_FADEOUT_BASE_LEVEL;
        int detail_mip_level = IDEAL_TEXELS_PER_DETAIL / LandscapeConfig.DETAIL_SIZE - 1;
        int detail_prefade_level = Math.max(detail_mip_level - base_level, 0);
        float detail_prefade = IDEAL_DETAIL_ALPHA * (float) Math.pow(LandscapeConfig.LANDSCAPE_DETAIL_FADEOUT_FACTOR,
                detail_prefade_level);

        Landscape landscape = new Landscape(num_players, config, detail_prefade, initial_unit_count,
                random_start_pos);
        Instant time_after = Instant.now();
        logger.fine(() -> "Landscape created in " + Duration.between(time_before, time_after));

        ProgressListener.progress();
        return new GeneratedLandscapeData(config, landscape);
    }
}
