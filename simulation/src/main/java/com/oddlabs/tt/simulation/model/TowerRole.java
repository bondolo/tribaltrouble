package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.state.Hunting;

/// Operational role for defensive tower buildings garrisoning a single mounted warrior.
public final class TowerRole implements BuildingRole {
  private final Building building;
  private final MountUnitContainer mountContainer;

  public TowerRole(Building building) {
    this.building = building;
    this.mountContainer = new MountUnitContainer(building);
  }

  @Override
  public UnitContainer getRawUnitContainer() {
    return mountContainer;
  }

  @Override
  public boolean canExitTower() {
    Unit unit = mountContainer.getUnit();
    return !building.isDead() && unit != null && building.getOwner().canExitTowers() && !unit.isStunned();
  }

  @Override
  public void exitTower() {
    if (canExitTower()) {
      mountContainer.exit();
    }
  }

  @Override
  public AttackScanFilter.Priority getAttackPriority() {
    return AttackScanFilter.Priority.TOWER;
  }

  @Override
  public void onTarget(Target target, Action action, boolean aggressive) {
    if (target != building) {
      boolean killFriendly = action == Action.ATTACK;
      Unit unit = mountContainer.getUnit();
      if (unit != null && unit.canAttack(target, killFriendly)) {
        unit.pushOrder(new Hunting((Selectable<?>) target, false, unit.getCurrentState()));
      }
    }
  }

  @Override
  public void animate(float dt) {
    mountContainer.animate(dt);
  }
}
