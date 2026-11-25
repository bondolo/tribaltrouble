package com.oddlabs.tt.simulation.model;

import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Playable unit types in the game. Supports mapping to legacy serialization integer values.
public enum UnitType {
  WARRIOR_ROCK(0, WeaponTier.ROCK),
  WARRIOR_IRON(1, WeaponTier.IRON),
  WARRIOR_RUBBER(2, WeaponTier.RUBBER),
  PEON(3, null),
  CHIEFTAIN(4, null);

  private final int value;
  private final @Nullable WeaponTier weaponTier;

  private static final Set<UnitType> VALUES = EnumSet.allOf(UnitType.class);

  UnitType(int value, @Nullable WeaponTier weaponTier) {
    this.value = value;
    this.weaponTier = weaponTier;
  }

  public int getValue() {
    return value;
  }

  public @Nullable WeaponTier getWeaponTier() {
    return weaponTier;
  }

  public static UnitType fromValue(int value) {
    for (UnitType type : VALUES) {
      if (type.value == value) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown legacy unit type value: " + value);
  }
}
