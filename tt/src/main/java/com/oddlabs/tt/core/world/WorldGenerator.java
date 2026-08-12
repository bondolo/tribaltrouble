package com.oddlabs.tt.core.world;

import com.oddlabs.tt.engine.resource.WorldInfo;
import org.jspecify.annotations.NonNull;

import java.io.Serializable;

public interface WorldGenerator extends Serializable {
    @NonNull
    WorldInfo<?> generate(int num_players, int initial_unit_count, float random_start_pos);

    int getMetersPerWorld();
}
