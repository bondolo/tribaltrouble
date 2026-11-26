package com.oddlabs.tt.simulation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/// Unit tests for extracted building capabilities, roles, and open role composition.
@NullMarked
final class BuildingCapabilitiesTest {

  private static final class ShipStyleRole implements BuildingRole, SupplyStorage {
    private final WorkerUnitContainer unitContainer;
    private final EnumMap<SupplyType, SupplyContainer> resources = new EnumMap<>(SupplyType.class);
    private final UnitDeployment deployment;

    ShipStyleRole(Building building) {
      this.unitContainer = new WorkerUnitContainer(building);
      for (SupplyType type : SupplyType.getValues()) {
        resources.put(type, new SupplyContainer(200));
      }
      EnumMap<DeployType, DeployContainer> deployContainers = new EnumMap<>(DeployType.class);
      deployContainers.put(DeployType.PEON, new DeployContainer(building, DeployType.PEON, null));
      deployContainers.put(
          DeployType.PEON_TRANSPORT_TREE,
          new DeployContainer(building, DeployType.PEON_TRANSPORT_TREE, resources.get(SupplyType.WOOD)));
      this.deployment = new UnitDeployment(building, deployContainers);
    }

    @Override
    public UnitContainer getRawUnitContainer() {
      return unitContainer;
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
    public Optional<SupplyContainer> getSupplyContainer(SupplyType type) {
      return Optional.ofNullable(resources.get(type));
    }

    @Override
    public void animate(float dt) {
      unitContainer.animate(dt);
      deployment.animate(dt);
    }

    @Override
    public int getStatusValue() {
      return unitContainer.getNumSupplies();
    }
  }

  @Test
  void testDeployTypeDurations() {
    assertEquals(1f, DeployType.ROCK_WARRIOR.getDeploySeconds());
    assertEquals(1.5f, DeployType.IRON_WARRIOR.getDeploySeconds());
    assertEquals(2f, DeployType.RUBBER_WARRIOR.getDeploySeconds());
    for (DeployType peonType :
        List.of(
            DeployType.PEON,
            DeployType.PEON_HARVEST_TREE,
            DeployType.PEON_TRANSPORT_TREE,
            DeployType.PEON_HARVEST_ROCK,
            DeployType.PEON_TRANSPORT_ROCK,
            DeployType.PEON_HARVEST_IRON,
            DeployType.PEON_TRANSPORT_IRON,
            DeployType.PEON_HARVEST_RUBBER,
            DeployType.PEON_TRANSPORT_RUBBER)) {
      assertEquals(0.5f, peonType.getDeploySeconds());
    }
  }

  @Test
  void testUnbuiltBuildingHasNoRoleAndQuartersPriority() {
    World world = createCleanWorld();
    Player viking = world.getPlayers().getFirst();
    Building armory = new Building(viking, viking.getRaceInfo().getBuildingTemplate(BuildingType.ARMORY), 16, 16);
    armory.place();

    assertFalse(armory.isBuilt());
    assertSame(BuildingRole.NONE, armory.getRole());
    assertEquals(AttackScanFilter.Priority.QUARTERS, armory.getAttackPriority());
    assertEquals(0, armory.getStatusValue());
    assertNull(armory.getRawUnitContainer());
    assertFalse(armory.hasSupplyStorage());
    assertFalse(armory.hasWeaponsWorkshop());
    assertFalse(armory.canReproduce());
    assertFalse(armory.canAcceptRally());
    assertFalse(armory.canDeployUnits(DeployType.PEON));
    assertFalse(armory.canBuildWeapons(WeaponTier.ROCK));
    assertFalse(armory.canSetRallyPoint());
  }

  @Test
  void testBuiltRolesWireExpectedCapabilities() {
    World world = createCleanWorld();
    Player viking = world.getPlayers().getFirst();

    Building armory = buildComplete(viking, BuildingType.ARMORY, 16, 16);
    ArmoryRole armoryRole = assertInstanceOf(ArmoryRole.class, armory.getRole());
    assertInstanceOf(WorkerUnitContainer.class, armoryRole.getRawUnitContainer());
    assertTrue(armoryRole.getDeployment().isPresent());
    assertTrue(armoryRole.getSupplyStorage().isPresent());
    assertTrue(armoryRole.getWeaponsWorkshop().isPresent());
    assertTrue(armoryRole.getChieftainContainer().isEmpty());
    assertTrue(armory.hasSupplyStorage());
    assertTrue(armory.hasWeaponsWorkshop());
    assertFalse(armory.canReproduce());
    assertTrue(armory.canAcceptRally());
    assertEquals(AttackScanFilter.Priority.ARMORY, armory.getAttackPriority());
    for (DeployType deployType : DeployType.values()) {
      assertTrue(armory.canDeployUnits(deployType));
      assertNotNull(armory.getDeployContainer(deployType));
    }
    armory.getUnitContainer().orElseThrow().increaseSupply(1);
    assertTrue(armory.canCreateHarvesters(1));
    assertFalse(armory.canCreateHarvesters(2));

    Building quarters = buildComplete(viking, BuildingType.QUARTERS, 32, 16);
    QuartersRole quartersRole = assertInstanceOf(QuartersRole.class, quarters.getRole());
    assertInstanceOf(ReproduceUnitContainer.class, quartersRole.getRawUnitContainer());
    assertTrue(quartersRole.getDeployment().isPresent());
    assertTrue(quartersRole.getChieftainContainer().isPresent());
    assertTrue(quartersRole.getSupplyStorage().isEmpty());
    assertTrue(quartersRole.getWeaponsWorkshop().isEmpty());
    assertFalse(quarters.hasSupplyStorage());
    assertFalse(quarters.hasWeaponsWorkshop());
    assertTrue(quarters.canReproduce());
    assertTrue(quarters.canAcceptRally());
    assertTrue(quarters.isValidRallyPoint(armory));
    assertEquals(AttackScanFilter.Priority.QUARTERS, quarters.getAttackPriority());
    assertTrue(quarters.canDeployUnits(DeployType.PEON));
    assertFalse(quarters.canDeployUnits(DeployType.ROCK_WARRIOR));
    assertTrue(quarters.canBuildChieftain());
    quarters.trainChieftain(true);
    assertTrue(quarters.isTrainingChieftain());
    assertTrue(viking.isTrainingChieftain());
    quarters.trainChieftain(false);
    assertFalse(quarters.isTrainingChieftain());
    assertFalse(viking.isTrainingChieftain());

    quarters.trainChieftain(true);
    assertTrue(viking.isTrainingChieftain());
    quarters.hit(quarters.getTemplate().getMaxHitPoints(), 1f, 0f, world.getPlayers().get(1));
    assertTrue(quarters.isDead());
    assertFalse(viking.isTrainingChieftain());

    Building tower = buildComplete(viking, BuildingType.TOWER, 48, 16);
    TowerRole towerRole = assertInstanceOf(TowerRole.class, tower.getRole());
    assertInstanceOf(MountUnitContainer.class, towerRole.getRawUnitContainer());
    assertTrue(towerRole.getDeployment().isEmpty());
    assertTrue(towerRole.getSupplyStorage().isEmpty());
    assertTrue(towerRole.getWeaponsWorkshop().isEmpty());
    assertTrue(towerRole.getChieftainContainer().isEmpty());
    assertFalse(tower.hasSupplyStorage());
    assertFalse(tower.hasWeaponsWorkshop());
    assertFalse(tower.canReproduce());
    assertTrue(tower.canAcceptRally());
    assertTrue(quarters.isValidRallyPoint(tower));
    assertEquals(AttackScanFilter.Priority.TOWER, tower.getAttackPriority());
    assertFalse(tower.canExitTower());

    Unit warrior =
        new Unit(
            viking,
            UnitGrid.coordinateFromGrid(44),
            UnitGrid.coordinateFromGrid(16),
            null,
            viking.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
    tower.getRawUnitContainer().enter(warrior);
    assertTrue(tower.canExitTower());
    tower.exitTower();
    assertFalse(warrior.isMounted());
    assertFalse(tower.canExitTower());
  }

  @Test
  void testOpenBuildingRoleSupportsStorageAndDeploymentWithoutWorkshop() {
    World world = createCleanWorld();
    Player viking = world.getPlayers().getFirst();
    BuildingTemplate base = viking.getRaceInfo().getBuildingTemplate(BuildingType.ARMORY);
    BuildingTemplate customTemplate =
        new BuildingTemplate(
            BuildingType.ARMORY,
            base.getPlacingSize(),
            base.getSmokeRadius(),
            base.getSmokeHeight(),
            base.getNumFragments(),
            base.getShadowDiameter(),
            base.getBuiltBounds(),
            base.getBuiltSelectionRadius(),
            base.getBuiltSelectionHeight(),
            base.getHalfbuiltBounds(),
            base.getHalfbuiltSelectionRadius(),
            base.getHalfbuiltSelectionHeight(),
            base.getStartBounds(),
            base.getStartSelectionRadius(),
            base.getStartSelectionHeight(),
            base.getMaxHitPoints(),
            ShipStyleRole::new,
            Set.of(Ability.TARGET),
            new float[] {0f, 1f, 3f},
            base.getMountOffset(),
            base.getDefenseChance(),
            base.getRally(),
            base.getChimney(),
            "CustomShipyard");

    Building customBuilding = new Building(viking, customTemplate, 24, 24);
    customBuilding.place();
    customBuilding.repair(customTemplate.getMaxHitPoints());

    assertInstanceOf(ShipStyleRole.class, customBuilding.getRole());
    assertTrue(customBuilding.hasSupplyStorage());
    assertFalse(customBuilding.hasWeaponsWorkshop());
    assertFalse(customBuilding.canReproduce());
    assertTrue(customBuilding.canAcceptRally());
    assertTrue(customBuilding.getSupplyContainer(SupplyType.WOOD).isPresent());
    assertTrue(customBuilding.canDeployUnits(DeployType.PEON));
    assertTrue(customBuilding.canDeployUnits(DeployType.PEON_TRANSPORT_TREE));
    assertFalse(customBuilding.canDeployUnits(DeployType.ROCK_WARRIOR));
    assertFalse(customBuilding.canBuildWeapons(WeaponTier.ROCK));
    assertFalse(customBuilding.canBuildChieftain());

    customBuilding.setRallyPoint(new LandscapeTarget(30, 30));
    assertTrue(customBuilding.hasRallyPoint());
    assertEquals(30, customBuilding.getRallyPoint().getGridX());
    assertEquals(30, customBuilding.getRallyPoint().getGridY());

    customBuilding.getSupplyContainer(SupplyType.WOOD).orElseThrow().increaseSupply(5);
    customBuilding.getUnitContainer().orElseThrow().increaseSupply(2);
    assertEquals(2, customBuilding.getStatusValue());

    customBuilding.deployUnits(DeployType.PEON_TRANSPORT_TREE, 1);
    assertEquals(1, customBuilding.getDeployContainer(DeployType.PEON_TRANSPORT_TREE).getNumSupplies());
  }

  private static Building buildComplete(Player owner, BuildingType type, int gridX, int gridY) {
    Building building = new Building(owner, owner.getRaceInfo().getBuildingTemplate(type), gridX, gridY);
    building.place();
    building.repair(building.getTemplate().getMaxHitPoints());
    return building;
  }

  private static World createCleanWorld() {
    int gridSize = 64;
    WorldParameters worldParams = new WorldParameters("1", 5, 20);
    List<PlayerInfo> playerInfos =
        List.of(new PlayerInfo(0, Race.VIKINGS, "VikingPlayer"), new PlayerInfo(1, Race.NATIVES, "NativePlayer"));
    Color.Linear[] playerColors =
        new Color.Linear[] {new Color.Linear(1f, 0f, 0f, 1f), new Color.Linear(0f, 0f, 1f, 1f)};
    return new World(worldParams, FlatLandscape.create(gridSize), PlayerConfig.from(playerInfos, playerColors));
  }
}
