package com.oddlabs.tt.landscape.worldgen;

import org.jspecify.annotations.NonNull;

import com.oddlabs.tt.resource.WorldInfo;
import java.io.Serializable;

public interface WorldGenerator extends Serializable {
    @NonNull
    WorldInfo<?> generate(int num_players, int initial_unit_count, float random_start_pos);

    int getMetersPerWorld();
}
