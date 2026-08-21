package com.oddlabs.tt.simulation.model;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.simulation.model.weapon.WeaponFactory;
import org.jspecify.annotations.Nullable;

/**
 * A template defining the base characteristics and assets for a unit type.
 */
public final class UnitTemplate extends Template {
    private final float meters_per_second;
    private final WeaponFactory weapon_factory;
    private final UnitVisualType visual_type;
    private final BoundingBox[] bounds;
    private final AnimationInfo.AnimationType[] anim_types;
    private final @Nullable UnitSupplyContainerFactory supply_container_factory;
    private final float death_pitch;
    private final float selection_radius;
    private final float selection_height;
    private final int max_hit_points;
    private final float stun_x;
    private final float stun_y;
    private final float stun_z;
    private final int status_value;

    public UnitTemplate(float selection_radius,
            float selection_height,
            Abilities abilities,
            float meters_per_second,
            WeaponFactory weapon_factory,
            UnitVisualType visual_type,
            BoundingBox[] bounds,
            AnimationInfo.AnimationType[] anim_types,
            float shadow_diameter,
            @Nullable UnitSupplyContainerFactory supply_container_factory,
            float death_pitch,
            float[] hit_offset_z,
            float no_detail_size,
            float defense_chance,
            String name,
            int max_hit_points,
            float stun_x,
            float stun_y,
            float stun_z,
            int status_value) {
        super(abilities, shadow_diameter, hit_offset_z, no_detail_size, defense_chance, name);
        this.selection_radius = selection_radius;
        this.selection_height = selection_height;
        this.meters_per_second = meters_per_second;
        this.weapon_factory = weapon_factory;
        this.visual_type = visual_type;
        this.bounds = bounds;
        this.anim_types = anim_types;
        this.supply_container_factory = supply_container_factory;

        this.death_pitch = death_pitch;
        this.max_hit_points = max_hit_points;
        this.stun_x = stun_x;
        this.stun_y = stun_y;
        this.stun_z = stun_z;
        this.status_value = status_value;
    }

    public float getSelectionRadius() {
        return selection_radius;
    }

    public float getSelectionHeight() {
        return selection_height;
    }

    public float getMetersPerSecond() {
        return meters_per_second;
    }

    public WeaponFactory getWeaponFactory() {
        return weapon_factory;
    }

    public UnitVisualType getVisualType() {
        return visual_type;
    }

    public BoundingBox[] getBounds() {
        return bounds;
    }

    /** {@return the animation type for the specified unit animation} */
    public AnimationInfo.AnimationType getAnimationType(Unit.Animation animation) {
        return anim_types[animation.ordinal()];
    }

    public UnitSupplyContainerFactory getUnitSupplyContainerFactory() {
        return supply_container_factory;
    }

    public float getDeathPitch() {
        return death_pitch;
    }

    public int getMaxHitPoints() {
        return max_hit_points;
    }

    public float getStunX() {
        return stun_x;
    }

    public float getStunY() {
        return stun_y;
    }

    public float getStunZ() {
        return stun_z;
    }

    public int getStatusValue() {
        return status_value;
    }
}
