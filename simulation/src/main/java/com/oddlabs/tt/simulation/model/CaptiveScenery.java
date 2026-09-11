package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.simulation.landscape.World;
import org.jspecify.annotations.Nullable;

/**
 * A world entity that represents a captive unit scenery item.
 */
public final class CaptiveScenery extends SceneryModel {
    private final Race race;
    private final UnitType unitType;

    public CaptiveScenery(World world, float x, float y, float dir_x, float dir_y,
            Race race, UnitType unitType, BoundingBox[] bounds, float shadow_diameter, boolean occupy,
            @Nullable String name, int animation, float seconds_per_animation_cycle, float anim_offset) {
        super(world, x, y, dir_x, dir_y, bounds, shadow_diameter, occupy, name, animation,
                seconds_per_animation_cycle, anim_offset);
        this.race = race;
        this.unitType = unitType;
    }

    public Race getRace() {
        return race;
    }

    public UnitType getUnitType() {
        return unitType;
    }
}
