package com.oddlabs.tt.simulation.player;

import org.jspecify.annotations.Nullable;

/// Simulation controller type determining starting unit placement and control mode.
public enum PlayerControllerType {
  HUMAN,
  AI_STANDARD,
  AI_TOWER_TUTORIAL,
  AI_CHIEFTAIN_TUTORIAL;

  /// Maps a legacy or lobby player slot to its corresponding simulation controller type.
  public static PlayerControllerType fromSlot(@Nullable PlayerSlot slot) {
    if (slot == null || slot.getType() != SlotType.AI) {
      return HUMAN;
    }
    return switch (slot.getAIDifficulty()) {
      case TOWER_TUTORIAL -> AI_TOWER_TUTORIAL;
      case CHIEFTAIN_TUTORIAL -> AI_CHIEFTAIN_TUTORIAL;
      default -> AI_STANDARD;
    };
  }
}
