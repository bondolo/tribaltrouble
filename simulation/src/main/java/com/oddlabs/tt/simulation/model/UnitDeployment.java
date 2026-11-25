package com.oddlabs.tt.simulation.model;

import java.util.EnumMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Capability managing unit deployment queues, rally point validation, and spawn timing for a building.
public final class UnitDeployment {
  private final Building building;
  private final EnumMap<DeployType, DeployContainer> containers = new EnumMap<>(DeployType.class);
  private Target rallyPoint;

  public UnitDeployment(Building building, Map<DeployType, DeployContainer> containers) {
    this.building = building;
    this.containers.putAll(containers);
    this.rallyPoint = building;
  }

  public static boolean isValidRallyPoint(Building source, Target target) {
    return target instanceof Building b
        && source.getOwner() == b.getOwner()
        && b.getAbilities().hasAbilities(Abilities.RALLY_TO);
  }

  public boolean hasRallyPoint() {
    return rallyPoint != building;
  }

  public Target getRallyPoint() {
    return rallyPoint;
  }

  public boolean isValidRallyPoint(Target target) {
    return isValidRallyPoint(building, target);
  }

  public boolean canSetRallyPoint() {
    return !building.isDead() && building.getOwner().canSetRallyPoints() && !containers.isEmpty();
  }

  public void setRallyPoint(Target target) {
    if (!canSetRallyPoint()) {
      return;
    }
    rallyPoint =
        isValidRallyPoint(target)
            ? target
            : building.getUnitGrid().findGridTarget(target.getGridX(), target.getGridY(), false);
  }

  public @Nullable Target resolveActiveRallyPoint() {
    if (hasRallyPoint() && rallyPoint.isDead()) {
      rallyPoint = building;
    }
    return hasRallyPoint() ? rallyPoint : null;
  }

  public boolean canDeploy(DeployType type) {
    return !building.isDead() && building.isBuilt() && containers.containsKey(type);
  }

  public @Nullable DeployContainer getContainer(DeployType type) {
    return containers.get(type);
  }

  public void deploy(DeployType type, int numUnits) {
    assert !building.isDead();
    if (!canDeploy(type) || numUnits == 0 || numUnits == Integer.MIN_VALUE) {
      return;
    }
    DeployContainer container = containers.get(type);
    if (container == null) {
      return;
    }
    building.getOwner().getWorld().updateGlobalChecksum(type.ordinal());
    building.getOwner().getWorld().updateGlobalChecksum(numUnits);
    container.orderSupply(numUnits);
  }

  public void animate(float dt) {
    int numDeploying = 0;
    for (DeployContainer deployContainer : containers.values()) {
      if (deployContainer.getNumSupplies() > 0) {
        numDeploying++;
      }
    }
    if (numDeploying > 0) {
      float amount = dt / numDeploying;
      for (DeployContainer deployContainer : containers.values()) {
        if (deployContainer.getNumSupplies() > 0) {
          deployContainer.deploy(amount);
        }
      }
    }
  }

  public void refundQueuedUnits() {
    for (DeployContainer deployContainer : containers.values()) {
      int result = building.getOwner().getUnitCountContainer().increaseSupply(-deployContainer.getNumSupplies());
      assert result == -deployContainer.getNumSupplies();
    }
  }
}
