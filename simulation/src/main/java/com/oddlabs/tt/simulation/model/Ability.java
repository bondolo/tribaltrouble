package com.oddlabs.tt.simulation.model;

/// Capabilities possessed by units and buildings.
public enum Ability {
  /// Can construct and repair buildings.
  BUILD,
  /// Can engage targets in combat.
  ATTACK,
  /// Can harvest raw resources from supplies.
  HARVEST,
  /// Can receive player target orders.
  TARGET,
  /// Can throw ranged weapons.
  THROW,
  /// Can cast chieftain magic spells.
  MAGIC
}
