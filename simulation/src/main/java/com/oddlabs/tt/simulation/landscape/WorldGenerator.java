package com.oddlabs.tt.simulation.landscape;


import java.io.Serializable;

/** Interface for procedural world map generators. */
public interface WorldGenerator<T> extends Serializable {
    T generate(int num_players, int initial_unit_count, float random_start_pos);

    int getMetersPerWorld();
}
