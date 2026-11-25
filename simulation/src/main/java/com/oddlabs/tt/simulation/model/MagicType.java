package com.oddlabs.tt.simulation.model;

/// Defines the types of chieftain magic spells available in the game.
public enum MagicType {
  POISON_FOG(40f),
  LIGHTNING_CLOUD(70f),
  STUN(40f),
  SONIC_BLAST(70f);

  private final float maxEnergy;

  MagicType(float maxEnergy) {
    this.maxEnergy = maxEnergy;
  }

  public float getMaxEnergy() {
    return maxEnergy;
  }
}
