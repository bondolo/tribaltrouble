package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

/**
 * Interface representing a harvestable resource supply in the simulation.
 */
public sealed interface Supply extends Occupant, ModelToolTip permits TreeSupply, SupplyModel {
    int HITS_PER_HARVEST = 10;

    @NonNull
    default String getName() {
        return Utils.getBundleString(ResourceBundle.getBundle(getClass().getName()), "name");
    }

    @NonNull
    SupplyType getSupplyType();

    boolean isEmpty();

    boolean hit();

    /** Create a new supply at the same location */
    @NonNull
    Supply respawn();

    @NonNull
    World getWorld();
}
