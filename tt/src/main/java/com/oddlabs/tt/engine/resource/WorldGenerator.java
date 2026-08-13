package com.oddlabs.tt.engine.resource;

import org.jspecify.annotations.NonNull;

import java.io.Serializable;

/** Interface for procedural world map generators. */
public interface WorldGenerator extends Serializable {
    @NonNull
    WorldInfo<?> generate(int num_players, int initial_unit_count, float random_start_pos);

    int getMetersPerWorld();
}
