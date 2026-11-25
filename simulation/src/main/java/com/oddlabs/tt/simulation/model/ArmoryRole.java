package com.oddlabs.tt.simulation.model;

import java.util.EnumMap;
import java.util.Optional;

/// Operational role for armory buildings storing resources, crafting weapons, and deploying workers or warriors.
public final class ArmoryRole implements BuildingRole, SupplyStorage {
  private static final int MAX_SUPPLY_COUNT = 200;

  private final Building building;
  private final WorkerUnitContainer workerContainer;
  private final EnumMap<SupplyType, SupplyContainer> resourceContainers = new EnumMap<>(SupplyType.class);
  private final WeaponsWorkshop weaponsWorkshop;
  private final UnitDeployment deployment;

  public ArmoryRole(Building building) {
    this.building = building;
    this.workerContainer = new WorkerUnitContainer(building);
    for (SupplyType type : SupplyType.getValues()) {
      resourceContainers.put(type, new SupplyContainer(MAX_SUPPLY_COUNT));
    }
    this.weaponsWorkshop = new WeaponsWorkshop(building, workerContainer, this, MAX_SUPPLY_COUNT);

    EnumMap<DeployType, DeployContainer> deployContainers = new EnumMap<>(DeployType.class);
    deployContainers.put(
        DeployType.ROCK_WARRIOR,
        new DeployContainer(
            building, DeployType.ROCK_WARRIOR, weaponsWorkshop.getWeaponContainerDirect(WeaponTier.ROCK)));
    deployContainers.put(
        DeployType.IRON_WARRIOR,
        new DeployContainer(
            building, DeployType.IRON_WARRIOR, weaponsWorkshop.getWeaponContainerDirect(WeaponTier.IRON)));
    deployContainers.put(
        DeployType.RUBBER_WARRIOR,
        new DeployContainer(
            building, DeployType.RUBBER_WARRIOR, weaponsWorkshop.getWeaponContainerDirect(WeaponTier.RUBBER)));
    deployContainers.put(DeployType.PEON, new DeployContainer(building, DeployType.PEON, null));
    deployContainers.put(
        DeployType.PEON_HARVEST_TREE, new DeployContainer(building, DeployType.PEON_HARVEST_TREE, null));
    deployContainers.put(
        DeployType.PEON_TRANSPORT_TREE,
        new DeployContainer(building, DeployType.PEON_TRANSPORT_TREE, resourceContainers.get(SupplyType.WOOD)));
    deployContainers.put(
        DeployType.PEON_HARVEST_ROCK, new DeployContainer(building, DeployType.PEON_HARVEST_ROCK, null));
    deployContainers.put(
        DeployType.PEON_TRANSPORT_ROCK,
        new DeployContainer(building, DeployType.PEON_TRANSPORT_ROCK, resourceContainers.get(SupplyType.ROCK)));
    deployContainers.put(
        DeployType.PEON_HARVEST_IRON, new DeployContainer(building, DeployType.PEON_HARVEST_IRON, null));
    deployContainers.put(
        DeployType.PEON_TRANSPORT_IRON,
        new DeployContainer(building, DeployType.PEON_TRANSPORT_IRON, resourceContainers.get(SupplyType.IRON)));
    deployContainers.put(
        DeployType.PEON_HARVEST_RUBBER, new DeployContainer(building, DeployType.PEON_HARVEST_RUBBER, null));
    deployContainers.put(
        DeployType.PEON_TRANSPORT_RUBBER,
        new DeployContainer(building, DeployType.PEON_TRANSPORT_RUBBER, resourceContainers.get(SupplyType.RUBBER)));
    this.deployment = new UnitDeployment(building, deployContainers);
  }

  @Override
  public Optional<SupplyContainer> getSupplyContainer(SupplyType type) {
    return Optional.ofNullable(resourceContainers.get(type));
  }

  @Override
  public UnitContainer getRawUnitContainer() {
    return workerContainer;
  }

  @Override
  public Optional<UnitDeployment> getDeployment() {
    return Optional.of(deployment);
  }

  @Override
  public Optional<SupplyStorage> getSupplyStorage() {
    return Optional.of(this);
  }

  @Override
  public Optional<WeaponsWorkshop> getWeaponsWorkshop() {
    return Optional.of(weaponsWorkshop);
  }

  @Override
  public boolean canCreateHarvesters(long totalRequested) {
    return !building.isDead()
        && building.isBuilt()
        && totalRequested >= 0
        && totalRequested <= workerContainer.getNumSupplies();
  }

  @Override
  public boolean isProducing() {
    return weaponsWorkshop.isProducing();
  }

  @Override
  public AttackScanFilter.Priority getAttackPriority() {
    return AttackScanFilter.Priority.ARMORY;
  }

  @Override
  public void animate(float dt) {
    workerContainer.animate(dt);
    weaponsWorkshop.animate(dt);
    deployment.animate(dt);
  }

  @Override
  public void onRemove() {
    weaponsWorkshop.remove();
  }

  @Override
  public int getStatusValue() {
    return workerContainer.getNumSupplies() + weaponsWorkshop.getWeaponStatusValue();
  }

  @Override
  public void printDebugInfo() {
    IO.println("Units = " + workerContainer.getNumSupplies());
    IO.println("Tree = " + resourceContainers.get(SupplyType.WOOD).getNumSupplies());
    IO.println("Rock = " + resourceContainers.get(SupplyType.ROCK).getNumSupplies());
    IO.println("Iron = " + resourceContainers.get(SupplyType.IRON).getNumSupplies());
    IO.println("Rubber = " + resourceContainers.get(SupplyType.RUBBER).getNumSupplies());
    IO.println("Rock Weapons = " + weaponsWorkshop.getWeaponSupplyCount(WeaponTier.ROCK));
    IO.println("Iron Weapons = " + weaponsWorkshop.getWeaponSupplyCount(WeaponTier.IRON));
    IO.println("Rubber Weapons = " + weaponsWorkshop.getWeaponSupplyCount(WeaponTier.RUBBER));
  }
}
