package com.oddlabs.tt.simulation.model;

import java.util.EnumSet;
import java.util.Set;

/// Game speed options.
public enum Gamespeed {
  PAUSE(0),
  SLOW(1),
  NORMAL(2),
  FAST(3),
  LUDICROUS(4);

  private final int value;

  private static final Set<Gamespeed> VALUES = EnumSet.allOf(Gamespeed.class);

  Gamespeed(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static Gamespeed fromValue(int value) {
    for (Gamespeed speed : VALUES) {
      if (speed.value == value) {
        return speed;
      }
    }
    throw new IllegalArgumentException("Unknown gamespeed value: " + value);
  }
}
