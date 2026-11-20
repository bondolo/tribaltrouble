package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.Building;

/// Fallback building transfer state locating a friendly supply container building and entering without deposit.
public final class Transporting implements UnitState {
  private final UnitState fallbackState;
  private final GiveUpTracker.Single giveUpTracker = new GiveUpTracker.Single();
  private boolean buildingTrackerActive;

  public Transporting(UnitState fallbackState) {
    this.fallbackState = fallbackState;
  }

  @Override
  public Transition decide(UnitContext context) {
    Building building = buildingTrackerActive ? context.getFoundSupplyBuilding() : null;
    if (building != null && context.isCloseEnough(0f, building)) {
      if (context.canEnterBuilding(building)) {
        context.enterBuildingWithoutDeposit(building);
        if (context.isDead()) {
          return Transition.stay();
        }
      }
      return Transition.to(fallbackState);
    }
    if (!giveUpTracker.shouldGiveUp()) {
      buildingTrackerActive = true;
      context.initSupplyBuildingFinderWalk();
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
