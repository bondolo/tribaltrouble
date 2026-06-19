package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

/**
 * Base abstract template defining properties shared by all unit and building types in the game.
 */
public abstract sealed class Template permits BuildingTemplate, UnitTemplate {
    private final @NonNull Abilities abilities;
    private final float @NonNull [] hit_offset_z;
    private final float defense_chance;
    private final @NonNull String name;

    protected Template(@NonNull Abilities abilities,
            float @NonNull [] hit_offset_z, float defense_chance, @NonNull String name) {
        this.abilities = abilities;
        this.hit_offset_z = hit_offset_z;
        this.defense_chance = defense_chance;
        this.name = name;
    }

    public final @NonNull String getName() {
        return name;
    }

    public final @NonNull Abilities getAbilities() {
        return abilities;
    }

    public final float getHitOffsetZ(int index) {
        return hit_offset_z[index];
    }

    public final float getDefenseChance() {
        return defense_chance;
    }
}
