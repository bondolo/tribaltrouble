package com.oddlabs.tt.simulation.model;

/// Unit deployment orders supported by buildings.
public enum DeployType {
  ROCK_WARRIOR(1f),
  IRON_WARRIOR(1.5f),
  RUBBER_WARRIOR(2f),
  PEON(0.5f),
  PEON_HARVEST_TREE(0.5f),
  PEON_TRANSPORT_TREE(0.5f),
  PEON_HARVEST_ROCK(0.5f),
  PEON_TRANSPORT_ROCK(0.5f),
  PEON_HARVEST_IRON(0.5f),
  PEON_TRANSPORT_IRON(0.5f),
  PEON_HARVEST_RUBBER(0.5f),
  PEON_TRANSPORT_RUBBER(0.5f);

  private final float deploySeconds;

  DeployType(float deploySeconds) {
    this.deploySeconds = deploySeconds;
  }

  public float getDeploySeconds() {
    return deploySeconds;
  }
}
