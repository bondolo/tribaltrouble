package com.oddlabs.tt.simulation.model;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Tier of finished weaponry manufactured in armories and equipped by warriors.
public enum WeaponTier {
  /// Tier-1 rock weaponry (Rock Axe / Rock Spear).
  ROCK(
      SupplyType.ROCK,
      DeployType.ROCK_WARRIOR,
      new Cost(
          Map.of(
              SupplyType.WOOD, 2,
              SupplyType.ROCK, 1)),
      40f,
      1,
      20f),

  /// Tier-2 iron weaponry (Iron Axe / Iron Spear).
  IRON(
      SupplyType.IRON,
      DeployType.IRON_WARRIOR,
      new Cost(
          Map.of(
              SupplyType.WOOD, 2,
              SupplyType.IRON, 1)),
      80f,
      3,
      25f),

  /// Tier-3 rubber weaponry (Rubber Axe / Rubber Spear).
  RUBBER(
      SupplyType.RUBBER,
      DeployType.RUBBER_WARRIOR,
      new Cost(
          Map.of(
              SupplyType.WOOD, 2,
              SupplyType.ROCK, 1,
              SupplyType.IRON, 1,
              SupplyType.RUBBER, 1)),
      120f,
      8,
      30f);

  private final SupplyType supplyType;
  private final DeployType deployType;
  private final Cost cost;
  private final float buildTime;
  private final int statusMultiplier;
  private final float metersPerSecond;

  WeaponTier(
      SupplyType supplyType,
      DeployType deployType,
      Cost cost,
      float buildTime,
      int statusMultiplier,
      float metersPerSecond) {
    this.supplyType = supplyType;
    this.deployType = deployType;
    this.cost = cost;
    this.buildTime = buildTime;
    this.statusMultiplier = statusMultiplier;
    this.metersPerSecond = metersPerSecond;
  }

  public Cost getCost() {
    return cost;
  }

  public SupplyType getSupplyType() {
    return supplyType;
  }

  public DeployType getDeployType() {
    return deployType;
  }

  public UnitType getUnitType() {
    return switch (this) {
      case ROCK -> UnitType.WARRIOR_ROCK;
      case IRON -> UnitType.WARRIOR_IRON;
      case RUBBER -> UnitType.WARRIOR_RUBBER;
    };
  }

  public float getBuildTime() {
    return buildTime;
  }

  public int getStatusMultiplier() {
    return statusMultiplier;
  }

  public float getMetersPerSecond() {
    return metersPerSecond;
  }

  public static @Nullable WeaponTier fromDeployType(DeployType deployType) {
    return switch (deployType) {
      case ROCK_WARRIOR -> ROCK;
      case IRON_WARRIOR -> IRON;
      case RUBBER_WARRIOR -> RUBBER;
      default -> null;
    };
  }
}
