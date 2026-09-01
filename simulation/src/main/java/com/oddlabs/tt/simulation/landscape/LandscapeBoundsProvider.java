package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.base.geom.BoundsProvider;
import com.oddlabs.tt.simulation.model.Terrain;

/**
 * Provides access to physical bounds of landscape elements (rocks, iron, plants, chickens)
 * for collision and bounding box calculations in the simulation.
 */
public interface LandscapeBoundsProvider {
    /** Iron/Rock supply fragment variations */
    int SUPPLY_FRAGMENT_COUNT = 5;

    BoundsProvider getRockBounds(int index);

    BoundsProvider getIronBounds(int index);

    BoundsProvider getPlantBounds(Terrain terrain, int index);

    BoundsProvider getChickenBounds();
}
