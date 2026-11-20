package com.oddlabs.tt.simulation.player;

import org.jspecify.annotations.Nullable;

/// Lobby slot type defining occupancy and player control mode.
public enum SlotType {
  OPEN(1),
  CLOSED(2),
  HUMAN(3),
  AI(4);

  private final int value;

  SlotType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static SlotType fromValue(int value) {
    return switch (value) {
      case 1 -> OPEN;
      case 2 -> CLOSED;
      case 3 -> HUMAN;
      case 4 -> AI;
      default -> throw new IllegalArgumentException("Unknown SlotType value: " + value);
    };
  }

  public static @Nullable SlotType fromValueOrNull(int value) {
    return switch (value) {
      case 1 -> OPEN;
      case 2 -> CLOSED;
      case 3 -> HUMAN;
      case 4 -> AI;
      default -> null;
    };
  }
}
