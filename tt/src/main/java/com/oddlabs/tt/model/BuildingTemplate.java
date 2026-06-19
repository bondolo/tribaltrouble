package com.oddlabs.tt.model;

import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

/**
 * Template defining the static properties, costs, and visual representations of a building type.
 */
public final class BuildingTemplate extends Template {
    private final @NonNull BuildingType building_type;
    private final int placing_size;
    private final @NonNull BoundingBox @NonNull [] built_bounds;
    private final @NonNull BoundingBox @NonNull [] halfbuilt_bounds;
    private final @NonNull BoundingBox @NonNull [] start_bounds;
    private final int max_hit_points;
    private final UnitContainerFactory unit_container_factory;
    private final float mount_offset;
    private final @NonNull Vector3fc rally;

    public BuildingTemplate(
            @NonNull BuildingType building_type,
            int placing_size,
            @NonNull BoundingBox @NonNull [] built_bounds,
            @NonNull BoundingBox @NonNull [] halfbuilt_bounds,
            @NonNull BoundingBox @NonNull [] start_bounds,
            int max_hit_points,
            UnitContainerFactory unit_container_factory,
            @NonNull Abilities abilities,
            float @NonNull [] hit_offset_z,
            float mount_offset,
            float defense_chance,
            @NonNull Vector3fc rally,
            @NonNull String name) {
        super(abilities, hit_offset_z, defense_chance, name);
        this.building_type = building_type;
        this.placing_size = placing_size;
        this.built_bounds = built_bounds;
        this.halfbuilt_bounds = halfbuilt_bounds;
        this.start_bounds = start_bounds;

        this.max_hit_points = max_hit_points;
        this.unit_container_factory = unit_container_factory;
        this.mount_offset = mount_offset;
        this.rally = rally;
    }

    public @NonNull BuildingType getBuildingType() {
        return building_type;
    }

    public int getPlacingSize() {
        return placing_size;
    }

    public @NonNull BoundingBox @NonNull [] getBuiltBounds() {
        return built_bounds;
    }

    public @NonNull BoundingBox @NonNull [] getStartBounds() {
        return start_bounds;
    }

    public @NonNull BoundingBox @NonNull [] getHalfbuiltBounds() {
        return halfbuilt_bounds;
    }

    public int getMaxHitPoints() {
        return max_hit_points;
    }

    public UnitContainerFactory getUnitContainerFactory() {
        return unit_container_factory;
    }

    public float getMountOffset() {
        return mount_offset;
    }

    public @NonNull Vector3fc getRally() {
        return rally;
    }
}
