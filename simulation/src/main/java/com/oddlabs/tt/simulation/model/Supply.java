package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.landscape.TreeSupply;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.pathfinder.Occupant;

public sealed interface Supply extends Occupant, ModelToolTip permits TreeSupply, SupplyModel {
    int HITS_PER_HARVEST = 10;

    SupplyType getSupplyType();

    boolean isEmpty();

    boolean hit();

    /** Create a new supply at the same location */
    Supply respawn();

    void animateSpawn(float t, float progress);

    void spawnComplete();

    default float getSpawnTime() {
        return 3.0f;
    }

    World getWorld();
}
