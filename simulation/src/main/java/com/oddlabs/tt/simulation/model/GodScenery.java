package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.simulation.landscape.World;
import org.jspecify.annotations.Nullable;

/**
 * A world entity that represents a deity statue scenery item.
 */
public final class GodScenery extends SceneryModel {
    private final Race race;

    public GodScenery(World world, float x, float y, float dir_x, float dir_y,
            Race race, BoundingBox[] bounds, float shadow_diameter, boolean occupy,
            @Nullable String name, int animation, float seconds_per_animation_cycle, float anim_offset) {
        super(world, x, y, dir_x, dir_y, bounds, shadow_diameter, occupy, name, animation,
                seconds_per_animation_cycle, anim_offset);
        this.race = race;
    }

    public Race getRace() {
        return race;
    }
}
