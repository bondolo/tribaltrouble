package com.oddlabs.tt.simulation.model;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// Open operational role capability attached to a building (`NONE` before construction completes).
public interface BuildingRole {
  /// Stateless default role used before a building finishes construction.
  BuildingRole NONE = new BuildingRole() {};

  default @Nullable UnitContainer getRawUnitContainer() {
    return null;
  }

  default Optional<UnitContainer> getUnitContainer() {
    return Optional.ofNullable(getRawUnitContainer());
  }

  default Optional<UnitDeployment> getDeployment() {
    return Optional.empty();
  }

  default Optional<SupplyStorage> getSupplyStorage() {
    return Optional.empty();
  }

  default Optional<WeaponsWorkshop> getWeaponsWorkshop() {
    return Optional.empty();
  }

  default Optional<ChieftainContainer> getChieftainContainer() {
    return Optional.empty();
  }

  default boolean canExitTower() {
    return false;
  }

  default void exitTower() {}

  default boolean canReproduce() {
    return false;
  }

  default boolean canAcceptRally() {
    return getRawUnitContainer() != null;
  }

  default boolean canCreateHarvesters(long totalRequested) {
    return false;
  }

  default boolean isProducing() {
    return false;
  }

  default boolean isTrainingChieftain() {
    return false;
  }

  default boolean canBuildChieftain() {
    return false;
  }

  default boolean canStopChieftain() {
    return false;
  }

  default void trainChieftain(boolean start) {}

  default void deployChieftain() {}

  default boolean spawnsUnralliedPeonsAsTransporters() {
    return false;
  }

  default AttackScanFilter.Priority getAttackPriority() {
    return AttackScanFilter.Priority.QUARTERS;
  }

  default void animate(float dt) {}

  default void onTarget(Target target, Action action, boolean aggressive) {
    getDeployment().ifPresent(deployment -> deployment.setRallyPoint(target));
  }

  default void onDestroyed() {}

  default void onRemoveDying() {
    UnitContainer unitContainer = getRawUnitContainer();
    if (unitContainer != null) {
      while (unitContainer.getNumSupplies() > 0) {
        Unit unit = unitContainer.exit();
        if (unit != null) {
          unit.removeNow();
        }
      }
    }
    getDeployment().ifPresent(UnitDeployment::refundQueuedUnits);
  }

  default void onRemove() {}

  default int getStatusValue() {
    return 0;
  }

  default void printDebugInfo() {}
}
