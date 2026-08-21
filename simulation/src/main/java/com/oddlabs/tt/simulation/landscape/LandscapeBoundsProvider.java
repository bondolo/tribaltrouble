package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.simulation.model.BoundsProvider;
import com.oddlabs.tt.simulation.model.Terrain;

/**
 * Provides access to visual bounds of landscape elements (rocks, iron, plants, chickens)
 * for collision and bounding box calculations in the simulation.
 * <p/>
 * For now, this interface is also used to smuggle a SpriteKey and downcasted.
 */
public interface LandscapeBoundsProvider {
    /** Iron/Rock supply fragment variations */
    int SUPPLY_FRAGMENT_COUNT = 5;

    BoundsProvider getRockBounds(int index);

    BoundsProvider getIronBounds(int index);

    BoundsProvider getPlantBounds(Terrain terrain, int index);

    BoundsProvider getChickenBounds();
}
