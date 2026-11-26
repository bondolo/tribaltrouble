package com.oddlabs.tt.simulation.model;

import java.util.Set;

/// Base abstract template defining properties shared by all unit and building types in the game.
public abstract sealed class Template permits BuildingTemplate, UnitTemplate {
  private final Set<Ability> abilities;
  private final float[] hit_offset_z;
  private final float defense_chance;
  private final float shadow_diameter;
  private final String name;

  protected Template(
      Set<Ability> abilities, float shadow_diameter, float[] hit_offset_z, float defense_chance, String name) {
    this.abilities = Set.copyOf(abilities);
    this.hit_offset_z = hit_offset_z;
    this.defense_chance = defense_chance;
    this.name = name;
    this.shadow_diameter = shadow_diameter;
  }

  public final String getName() {
    return name;
  }

  public final Set<Ability> getAbilities() {
    return abilities;
  }

  public final boolean hasAbility(Ability ability) {
    return abilities.contains(ability);
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
