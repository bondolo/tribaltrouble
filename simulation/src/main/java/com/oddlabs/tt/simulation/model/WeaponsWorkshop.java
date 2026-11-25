package com.oddlabs.tt.simulation.model;

import java.util.EnumMap;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// Capability managing weapon stockpiles, build orders, and worker weapon production for a building.
public final class WeaponsWorkshop {
  private final Building building;
  private final EnumMap<WeaponTier, SupplyContainer> weaponContainers = new EnumMap<>(WeaponTier.class);
  private final EnumMap<WeaponTier, BuildProductionContainer> buildContainers = new EnumMap<>(WeaponTier.class);
  private final WeaponsForge forge;

  public WeaponsWorkshop(
      Building building, WorkerUnitContainer workerContainer, SupplyStorage supplyStorage, int maxSupplyCount) {
    this.building = building;
    for (WeaponTier tier : WeaponTier.values()) {
      SupplyContainer weaponContainer = new SupplyContainer(maxSupplyCount);
      weaponContainers.put(tier, weaponContainer);
      buildContainers.put(tier, new BuildProductionContainer(weaponContainer, supplyStorage, tier));
    }
    BuildProductionContainer[] productionContainers = buildContainers.values().toArray(BuildProductionContainer[]::new);
    this.forge = new WeaponsForge(building, workerContainer, productionContainers);
  }

  public Optional<SupplyContainer> getWeaponContainer(WeaponTier tier) {
    return Optional.ofNullable(weaponContainers.get(tier));
  }

  public @Nullable SupplyContainer getWeaponContainerDirect(WeaponTier tier) {
    return weaponContainers.get(tier);
  }

  public int getWeaponSupplyCount(WeaponTier tier) {
    SupplyContainer container = weaponContainers.get(tier);
    return container != null ? container.getNumSupplies() : 0;
  }

  public Optional<BuildProductionContainer> getWeaponBuildContainer(WeaponTier tier) {
    return Optional.ofNullable(buildContainers.get(tier));
  }

  public boolean canBuildWeapons(WeaponTier tier) {
    return !building.isDead() && building.isBuilt() && buildContainers.containsKey(tier);
  }

  public void buildWeapons(WeaponTier tier, int numWeapons, boolean infinite) {
    assert !building.isDead();
    if (!canBuildWeapons(tier) || (!infinite && numWeapons == 0) || numWeapons == Integer.MIN_VALUE) {
      return;
    }
    building.getOwner().getWorld().updateGlobalChecksum(infinite ? numWeapons : 1000000);
    BuildProductionContainer buildContainer = buildContainers.get(tier);
    if (buildContainer != null) {
      buildContainer.orderSupply(numWeapons, infinite);
    }
  }

  public boolean isProducing() {
    return forge.isProducing();
  }

  public void animate(float dt) {
    forge.animate(dt);
  }

  public void remove() {
    forge.remove();
  }

  public int getWeaponStatusValue() {
    int total = 0;
    for (WeaponTier tier : WeaponTier.values()) {
      total += getWeaponSupplyCount(tier) * tier.getStatusMultiplier();
    }
    return total;
  }
}
