package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.simulation.landscape.World;

/**
 * A world entity that represents a static rally point marker scenery item.
 */
public final class RallyPointScenery extends SceneryModel {
    private final Race race;

    public RallyPointScenery(World world, float x, float y, float dir_x, float dir_y,
            Race race, BoundingBox[] bounds) {
        super(world, x, y, dir_x, dir_y, bounds);
        this.race = race;
    }

    public Race getRace() {
        return race;
    }
}
