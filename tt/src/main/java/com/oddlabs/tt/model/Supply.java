package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public non-sealed interface Supply extends Occupant, ModelToolTip {
    int HITS_PER_HARVEST = 10;

    @NonNull
    SpriteKey getStatusSprite(@NonNull RacesResources resources);

    boolean isEmpty();

    boolean hit();

    /** Create a new supply at the same location */
    @NonNull
    Supply respawn();

    void animateSpawn(float t, float progress);

    void spawnComplete();

    default float getSpawnTime() {
        return 3.0f;
    }

    @NonNull
    World getWorld();
}
