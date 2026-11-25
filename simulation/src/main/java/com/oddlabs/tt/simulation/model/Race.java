package com.oddlabs.tt.simulation.model;

import org.jspecify.annotations.Nullable;

/// Playable races in the game.
public enum Race {
  NATIVES(0),
  VIKINGS(1);

  private final int value;

  Race(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static Race fromValue(int value) {
    return switch (value) {
      case 0 -> NATIVES;
      case 1 -> VIKINGS;
      default -> throw new IllegalArgumentException("Unknown race value: " + value);
    };
  }

  public static @Nullable Race fromValueOrNull(int value) {
    return switch (value) {
      case 0 -> NATIVES;
      case 1 -> VIKINGS;
      default -> null;
    };
  }
}
