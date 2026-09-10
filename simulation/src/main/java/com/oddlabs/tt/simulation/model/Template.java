package com.oddlabs.tt.simulation.model;


/**
 * Base abstract template defining properties shared by all unit and building types in the game.
 */
public abstract sealed class Template permits BuildingTemplate, UnitTemplate {
    private final Abilities abilities;
    private final float[] hit_offset_z;
    private final float defense_chance;
    private final float shadow_diameter;
    private final String name;

    protected Template(Abilities abilities, float shadow_diameter,
            float[] hit_offset_z, float defense_chance, String name) {
        this.abilities = abilities;
        this.hit_offset_z = hit_offset_z;
        this.defense_chance = defense_chance;
        this.name = name;
        this.shadow_diameter = shadow_diameter;
    }

    public final String getName() {
        return name;
    }

    public final Abilities getAbilities() {
        return abilities;
    }

    public final float getShadowDiameter() {
        return shadow_diameter;
    }

    public final float getHitOffsetZ(int index) {
        return hit_offset_z[index];
    }

    public final float getDefenseChance() {
        return defense_chance;
    }
}
