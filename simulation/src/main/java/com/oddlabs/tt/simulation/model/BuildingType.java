package com.oddlabs.tt.simulation.model;

import java.util.EnumSet;
import java.util.Set;

/// Represents the types of constructible buildings in the game. Supports mapping to legacy serialization integer
/// values.
public enum BuildingType {
  QUARTERS(0),
  ARMORY(1),
  TOWER(2);

  private final int value;

  private static final Set<BuildingType> VALUES = EnumSet.allOf(BuildingType.class);

  BuildingType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static BuildingType fromValue(int value) {
    for (BuildingType type : VALUES) {
      if (type.value == value) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown legacy building type value: " + value);
  }
}
