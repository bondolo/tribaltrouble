package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Supply;
import com.oddlabs.tt.simulation.model.SupplyType;
import org.jspecify.annotations.Nullable;

/// Resource gathering loop coordinating `Harvesting` extraction and supply-building dropoff.
public final class Gathering implements UnitState {
  private enum Phase {
    HARVEST,
    DROPOFF
  }

  private final SupplyType supplyType;
  private final UnitState fallbackState;
  private final GiveUpTracker<Phase> giveUpTracker = new GiveUpTracker<>(Phase.class);
  private @Nullable Supply supply;
  private boolean buildingTrackerActive;

  public Gathering(@Nullable Supply supply, SupplyType supplyType, UnitState fallbackState) {
    this.supply = supply;
    this.supplyType = supplyType;
    this.fallbackState = fallbackState;
  }

  @Override
  public Transition decide(UnitContext context) {
    if (context.getCarriedSupplyCount() > 0 && context.getCarriedSupplyType() == supplyType) {
      return dropoff(context);
    }
    return gather(context);
  }

  private Transition gather(UnitContext context) {
    giveUpTracker.reset(Phase.DROPOFF);
    if (supply != null && supply.isDead()) {
      supply = null;
      giveUpTracker.reset(Phase.HARVEST);
    }
    if (supply != null && context.isCloseEnough(context.getAttackRange(supply), supply)) {
      return Transition.to(new Harvesting(supply, supplyType, this));
    }
    if (!giveUpTracker.shouldGiveUp(Phase.HARVEST)) {
      if (supply == null) {
        return Transition.to(new Harvesting(null, supplyType, this));
      }
      context.initWalkTarget(supply, 0f);
      return Transition.walk(false);
    }
    return Transition.to(new Transporting(fallbackState));
  }

  private Transition dropoff(UnitContext context) {
    giveUpTracker.reset(Phase.HARVEST);
    Building building = buildingTrackerActive ? context.getFoundSupplyBuilding() : null;
    if (building != null && context.isCloseEnough(0f, building)) {
      boolean hasRemaining = context.depositSupplyIntoBuilding(building);
      if (hasRemaining) {
        return context.isLegacyDeterministic()
            ? Transition.popAndPush(fallbackState, resolvedFallback -> new EnteringBuilding(building, resolvedFallback))
            : Transition.to(new EnteringBuilding(building, fallbackState));
      }
      return gather(context);
    }
    if (!giveUpTracker.shouldGiveUp(Phase.DROPOFF)) {
      buildingTrackerActive = true;
      context.initSupplyBuildingFinderWalk();
      return Transition.walk(false);
    }
    return Transition.to(fallbackState);
  }

  SupplyType getSupplyType() {
    return supplyType;
  }

  @Override
  public UnitState getFallbackState() {
    return fallbackState;
  }

  @Override
  public void resetGiveUpCounters() {
    giveUpTracker.resetAll();
  }
}
