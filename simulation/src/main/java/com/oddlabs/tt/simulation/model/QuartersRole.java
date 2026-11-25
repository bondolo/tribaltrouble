package com.oddlabs.tt.simulation.model;

import java.util.Map;
import java.util.Optional;

/// Operational role for quarters buildings handling peon reproduction, deployment, and chieftain training.
public final class QuartersRole implements BuildingRole {
  private final Building building;
  private final ChieftainContainer chieftainContainer;
  private final ReproduceUnitContainer reproduceContainer;
  private final UnitDeployment deployment;

  public QuartersRole(Building building) {
    this.building = building;
    this.chieftainContainer = new ChieftainContainer(building, this::resetReproductionProgress);
    this.reproduceContainer = new ReproduceUnitContainer(building, chieftainContainer);
    this.deployment =
        new UnitDeployment(building, Map.of(DeployType.PEON, new DeployContainer(building, DeployType.PEON, null)));
  }

  private void resetReproductionProgress() {
    reproduceContainer.resetProgress();
  }

  @Override
  public UnitContainer getRawUnitContainer() {
    return reproduceContainer;
  }

  @Override
  public Optional<UnitDeployment> getDeployment() {
    return Optional.of(deployment);
  }

  @Override
  public Optional<ChieftainContainer> getChieftainContainer() {
    return Optional.of(chieftainContainer);
  }

  @Override
  public boolean canBuildChieftain() {
    return !building.isDead()
        && building.getOwner().canBuildChieftains()
        && !building.getOwner().hasActiveChieftain()
        && !building.getOwner().isTrainingChieftain();
  }

  @Override
  public boolean canStopChieftain() {
    return !building.isDead() && chieftainContainer.isTraining();
  }

  @Override
  public boolean isTrainingChieftain() {
    return !building.isDead() && chieftainContainer.isTraining();
  }

  @Override
  public void trainChieftain(boolean start) {
    if (canBuildChieftain() && start) {
      chieftainContainer.startTraining();
      building.getOwner().setTrainingChieftain(true);
    } else if (canStopChieftain() && !start) {
      chieftainContainer.stopTraining();
      building.getOwner().setTrainingChieftain(false);
    }
  }

  @Override
  public void deployChieftain() {
    chieftainContainer.stopTraining();
    building.getOwner().setTrainingChieftain(false);
    Unit chieftain = building.spawnUnit(null, building.getOwner().getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN));
    building.getOwner().setActiveChieftain(chieftain);
  }

  @Override
  public boolean spawnsUnralliedPeonsAsTransporters() {
    return true;
  }

  @Override
  public void onDestroyed() {
    if (chieftainContainer.isTraining()) {
      building.getOwner().setTrainingChieftain(false);
    }
  }

  @Override
  public void animate(float dt) {
    reproduceContainer.animate(dt);
    deployment.animate(dt);
  }

  @Override
  public int getStatusValue() {
    return reproduceContainer.getNumSupplies();
  }

  @Override
  public void printDebugInfo() {
    IO.println("Units = " + reproduceContainer.getNumSupplies());
  }
}
