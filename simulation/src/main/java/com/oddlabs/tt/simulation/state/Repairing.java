package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Unit;

/// Construction and repair state consuming wood via `Harvesting`, walking to the building, and hammering repairs.
public final class Repairing implements UnitState {
  private enum Phase {
    HARVEST,
    REPAIR
  }

  private final Building building;
  private final UnitState fallbackState;
  private final GiveUpTracker<Phase> giveUpTracker = new GiveUpTracker<>(Phase.class);
  private final RepairCyclePhase repairPhase = new RepairCyclePhase();

  public Repairing(Building building, UnitState fallbackState) {
    this.building = building;
    this.fallbackState = fallbackState;
  }

  public Building getBuilding() {
    return building;
  }

  @Override
  public Transition decide(UnitContext context) {
    repairPhase.reset();
    if (building.isDead()) {
      return Transition.to(fallbackState);
    }
    if (context.getCarriedSupplyType() == SupplyType.WOOD && context.getCarriedSupplyCount() > 0) {
      giveUpTracker.reset(Phase.HARVEST);
      if (context.isCloseEnough(0f, building)) {
        if (building.isDamaged()) {
          repairPhase.start(building, context);
          return Transition.stay();
        }
        if (context.isFriendlySupplyContainerBuilding(building)) {
          return Transition.to(new EnteringBuilding(building, fallbackState));
        }
        return Transition.to(fallbackState);
      }
      if (giveUpTracker.shouldGiveUp(Phase.REPAIR)) {
        return Transition.to(fallbackState);
      }
      context.initWalkTarget(building, 0f);
      return Transition.walk(false);
    }
    giveUpTracker.reset(Phase.REPAIR);
    if (!giveUpTracker.shouldGiveUp(Phase.HARVEST)) {
      return Transition.to(new Harvesting(null, SupplyType.WOOD, this));
    }
    return Transition.to(fallbackState);
  }

  @Override
  public TickResult tick(float dt, UnitContext context) {
    return repairPhase.isActive() ? repairPhase.tick(dt, context, building) : TickResult.DONE;
  }

  boolean isRepairActive() {
    return repairPhase.isActive();
  }

  @Override
  public UnitState getFallbackState() {
    return fallbackState;
  }

  @Override
  public void resetGiveUpCounters() {
    giveUpTracker.resetAll();
  }

  /// Consumes 1 wood upfront and executes up to `REPAIRS_PER_SUPPLY` 1-second repair cycles.
  static final class RepairCyclePhase {
    static final int REPAIRS_PER_SUPPLY = 5;
    private static final float SECONDS_PER_ANIMATION_CYCLE = 1f;

    private boolean active;
    private float animTime;
    private int repairs;
    private boolean sound;

    void reset() {
      this.active = false;
    }

    void start(Building building, UnitContext context) {
      this.active = true;
      this.repairs = 0;
      context.aimAtTarget(building);
      restartAnimation(context);
      context.consumeOneWoodForRepair();
    }

    boolean isActive() {
      return active;
    }

    private void restartAnimation(UnitContext context) {
      animTime = 0f;
      sound = false;
      context.switchAnimation(1f / SECONDS_PER_ANIMATION_CYCLE, Unit.Animation.THROWING);
    }

    TickResult tick(float dt, UnitContext context, Building building) {
      animTime += dt;
      if (animTime > context.getWeaponSecondsPerRelease(1f / SECONDS_PER_ANIMATION_CYCLE) && !sound) {
        sound = true;
        context.onRepairSwing();
      }
      if (animTime > SECONDS_PER_ANIMATION_CYCLE) {
        restartAnimation(context);
        repairs++;
        if (building.isDead() || (building.isBuilt() && !building.isDamaged())) {
          return TickResult.DONE;
        }
        context.applyRepairPoint(building);
      }
      return repairs == REPAIRS_PER_SUPPLY ? TickResult.DONE : TickResult.INTERRUPTIBLE;
    }
  }
}
