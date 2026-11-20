package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.Building;

/// Construction placement state walking to a building site, placing it when legal, and transitioning to repair.
public final class PlacingBuilding implements UnitState {
  private final Building building;
  private final UnitState fallbackState;
  private final GiveUpTracker.Single giveUpTracker = new GiveUpTracker.Single();

  public PlacingBuilding(Building building, UnitState fallbackState) {
    this.building = building;
    this.fallbackState = fallbackState;
  }

  public Building getBuilding() {
    return building;
  }

  @Override
  public Transition decide(UnitContext context) {
    if (building.isDead()) {
      return Transition.to(fallbackState);
    }
    if (building.isPlaced()) {
      return building.isDamaged()
          ? Transition.to(new Repairing(building, fallbackState))
          : Transition.to(fallbackState);
    }
    if (context.isCloseEnough(building.getSize(), building)) {
      if (context.isBuildingPlacementLegal(building)) {
        context.placeBuilding(building);
        return Transition.to(new Repairing(building, fallbackState));
      }
      return Transition.to(fallbackState);
    }
    if (!giveUpTracker.shouldGiveUp()) {
      context.initWalkTarget(building, building.getSize());
      return Transition.walk(false);
    }
    return Transition.to(fallbackState);
  }

  @Override
  public UnitState getFallbackState() {
    return fallbackState;
  }

  @Override
  public void resetGiveUpCounters() {
    giveUpTracker.reset();
  }
}
