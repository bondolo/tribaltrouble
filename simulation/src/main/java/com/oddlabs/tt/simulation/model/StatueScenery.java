package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.simulation.landscape.World;
import org.jspecify.annotations.Nullable;

/**
 * A world entity that represents a decorative statue or treasure scenery item.
 */
public final class StatueScenery extends SceneryModel {
    private final int treasureIndex;

    public StatueScenery(World world, float x, float y, float dir_x, float dir_y,
            int treasureIndex, BoundingBox[] bounds, float shadow_diameter, boolean occupy, @Nullable String name) {
        super(world, x, y, dir_x, dir_y, bounds, shadow_diameter, occupy, name);
        this.treasureIndex = treasureIndex;
    }

    public int getTreasureIndex() {
        return treasureIndex;
    }
}
