package com.oddlabs.tt.landscape;

import com.oddlabs.tt.model.BoundsProvider;
import com.oddlabs.tt.model.Terrain;
import org.jspecify.annotations.NonNull;

/**
 * Provides access to visual bounds of landscape elements (rocks, iron, plants, chickens)
 * for collision and bounding box calculations in the simulation.
 */
public interface LandscapeBoundsProvider {
    @NonNull
    BoundsProvider getRockBounds(int index);

    @NonNull
    BoundsProvider getIronBounds(int index);

    @NonNull
    BoundsProvider getPlantBounds(Terrain terrain, int index);

    @NonNull
    BoundsProvider getChickenBounds();
}
