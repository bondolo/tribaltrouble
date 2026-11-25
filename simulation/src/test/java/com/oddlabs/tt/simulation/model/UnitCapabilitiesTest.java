package com.oddlabs.tt.simulation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.state.Idling;
import com.oddlabs.tt.simulation.state.UnitState;
import com.oddlabs.util.Color;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/// Unit tests for extracted unit capabilities, mount descriptors, and role predicates.
@NullMarked
final class UnitCapabilitiesTest {

  private record CustomBoatMount(Building host, float exitX, float exitY) implements UnitMount {
    @Override
    public float offsetZ() {
      return 1.5f;
    }

    @Override
    public float rangeBonus() {
      return 4f;
    }

    @Override
    public boolean forwardsDamageToHost() {
      return false;
    }

    @Override
    public float attackSpeedMultiplier() {
      return 1f;
    }

    @Override
    public int statusValueMultiplier() {
      return 2;
    }

    @Override
    public UnitState mountedDefaultState() {
      return new Idling(AttackScanFilter.UNIT_RANGE, false);
    }

    @Override
    public float unmountPositionX() {
      return exitX;
    }

    @Override
    public float unmountPositionY() {
      return exitY;
    }
  }

  @Test
  void testMagicTypeMaxEnergyValues() {
    assertEquals(40f, MagicType.POISON_FOG.getMaxEnergy());
    assertEquals(70f, MagicType.LIGHTNING_CLOUD.getMaxEnergy());
    assertEquals(40f, MagicType.STUN.getMaxEnergy());
    assertEquals(70f, MagicType.SONIC_BLAST.getMaxEnergy());
  }

  @Test
  void testMagicCapabilityRechargeClampingAndCastConsumption() {
    MagicCapability capability = new MagicCapability();
    assertEquals(0f, capability.getEnergy(MagicType.STUN));
    assertEquals(0f, capability.getProgress(MagicType.STUN));
    assertFalse(capability.isReady(MagicType.STUN));
    assertNull(capability.getLastMagicType());

    capability.increaseEnergy(MagicType.STUN, 10f);
    assertEquals(10f, capability.getEnergy(MagicType.STUN));
    assertEquals(0.25f, capability.getProgress(MagicType.STUN), 1e-6f);
    assertFalse(capability.isReady(MagicType.STUN));

    capability.increaseEnergy(MagicType.STUN, 100f);
    assertEquals(40f, capability.getEnergy(MagicType.STUN));
    assertEquals(1f, capability.getProgress(MagicType.STUN));
    assertTrue(capability.isReady(MagicType.STUN));

    capability.maxEnergy(MagicType.SONIC_BLAST);
    assertTrue(capability.isReady(MagicType.SONIC_BLAST));

    capability.consumeAll(MagicType.STUN);
    assertEquals(0f, capability.getEnergy(MagicType.STUN));
    assertEquals(0f, capability.getEnergy(MagicType.SONIC_BLAST));
    assertEquals(MagicType.STUN, capability.getLastMagicType());
  }

  @Test
  void testRoleScopedMagicAllocationAndRolePredicates() {
    assertTrue(UnitType.PEON.isPeon());
    assertFalse(UnitType.PEON.isWarrior());
    assertFalse(UnitType.PEON.isChieftain());

    for (UnitType warriorType : List.of(UnitType.WARRIOR_ROCK, UnitType.WARRIOR_IRON, UnitType.WARRIOR_RUBBER)) {
      assertFalse(warriorType.isPeon());
      assertTrue(warriorType.isWarrior());
      assertFalse(warriorType.isChieftain());
    }

    assertFalse(UnitType.CHIEFTAIN.isPeon());
    assertFalse(UnitType.CHIEFTAIN.isWarrior());
    assertTrue(UnitType.CHIEFTAIN.isChieftain());

    World world = createCleanWorld();
    Player viking = world.getPlayers().getFirst();

    Unit peon = spawnUnit(viking, UnitType.PEON, 12, 12);
    Unit warrior = spawnUnit(viking, UnitType.WARRIOR_ROCK, 14, 12);
    Unit chieftain = spawnUnit(viking, UnitType.CHIEFTAIN, 16, 12);

    assertTrue(peon.isPeon());
    assertFalse(peon.isWarrior());
    assertFalse(peon.isChieftain());
    assertTrue(peon.getMagicCapability().isEmpty());

    assertFalse(warrior.isPeon());
    assertTrue(warrior.isWarrior());
    assertFalse(warrior.isChieftain());
    assertTrue(warrior.getMagicCapability().isEmpty());

    assertFalse(chieftain.isPeon());
    assertFalse(chieftain.isWarrior());
    assertTrue(chieftain.isChieftain());
    assertTrue(chieftain.getMagicCapability().isPresent());

    // Facade methods on non-casters remain safe no-ops/false
    peon.increaseMagicEnergy(MagicType.STUN, 10f);
    peon.maxMagicEnergy(MagicType.STUN);
    assertEquals(0f, peon.getMagicProgress(MagicType.STUN));
    assertFalse(peon.canDoMagic(MagicType.STUN));
    assertNull(peon.getLastMagicType());
    peon.doMagic(MagicType.STUN, true);
  }

  @Test
  void testTowerMountLifecycleAndDamageForwarding() {
    World world = createCleanWorld();
    Player viking = world.getPlayers().getFirst();
    Player enemy = world.getPlayers().get(1);

    Building tower = new Building(viking, viking.getRaceInfo().getBuildingTemplate(BuildingType.TOWER), 16, 16);
    tower.place();
    tower.repair(tower.getTemplate().getMaxHitPoints());

    Unit warrior = spawnUnit(viking, UnitType.WARRIOR_ROCK, 20, 16);
    Unit target = spawnUnit(enemy, UnitType.PEON, 24, 16);

    float baseRange = warrior.getRange(target);
    int baseStatus = warrior.getStatusValue();
    assertFalse(warrior.isMounted());
    assertTrue(warrior.getMount().isEmpty());
    assertEquals(0f, warrior.getMountOffset());
    assertEquals(0f, warrior.getOffsetZ());

    tower.getRawUnitContainer().enter(warrior);

    assertTrue(warrior.isMounted());
    TowerMount mount = assertInstanceOf(TowerMount.class, warrior.getMount().orElseThrow());
    assertEquals(tower, mount.host());
    assertEquals(tower.getTemplate().getMountOffset(), warrior.getMountOffset());
    assertEquals(tower.getOffsetZ() + tower.getTemplate().getMountOffset(), warrior.getOffsetZ());
    assertEquals(baseStatus * TowerMount.STATUS_VALUE_MULTIPLIER, warrior.getStatusValue());
    assertEquals(baseRange + MountUnitContainer.ATTACK_RANGE_INCREASE, warrior.getRange(target), 1e-4f);

    int initialTowerHp = tower.getHitPoints();
    warrior.hit(10, 1f, 0f, enemy);
    assertEquals(initialTowerHp - 10, tower.getHitPoints());
    assertFalse(warrior.isDead());

    tower.exitTower();

    assertFalse(warrior.isMounted());
    assertTrue(warrior.getMount().isEmpty());
    assertEquals(0f, warrior.getMountOffset());
    assertEquals(0f, warrior.getOffsetZ());
    assertEquals(baseStatus, warrior.getStatusValue());
    assertEquals(baseRange, warrior.getRange(target), 1e-4f);
  }

  @Test
  void testCustomUnitMountWithoutDamageForwardingAndCustomExit() {
    World world = createCleanWorld();
    Player viking = world.getPlayers().getFirst();
    Player enemy = world.getPlayers().get(1);

    Building host = new Building(viking, viking.getRaceInfo().getBuildingTemplate(BuildingType.QUARTERS), 16, 16);
    host.place();
    host.repair(host.getTemplate().getMaxHitPoints());

    float customExitX = UnitGrid.coordinateFromGrid(26);
    float customExitY = UnitGrid.coordinateFromGrid(26);

    Unit directHitWarrior = spawnUnit(viking, UnitType.WARRIOR_ROCK, 12, 12);
    directHitWarrior.mount(new CustomBoatMount(host, customExitX, customExitY));
    int hostHpBefore = host.getHitPoints();
    directHitWarrior.hit(5, 1f, 0f, enemy);
    assertTrue(directHitWarrior.isDead());
    assertEquals(hostHpBefore, host.getHitPoints());

    Unit exitingWarrior = spawnUnit(viking, UnitType.WARRIOR_IRON, 14, 12);
    int baseStatus = exitingWarrior.getStatusValue();
    exitingWarrior.mount(new CustomBoatMount(host, customExitX, customExitY));
    assertEquals(baseStatus * 2, exitingWarrior.getStatusValue());

    exitingWarrior.unmount();
    assertFalse(exitingWarrior.isMounted());
    assertEquals(26, exitingWarrior.getGridX());
    assertEquals(26, exitingWarrior.getGridY());
    assertEquals(baseStatus, exitingWarrior.getStatusValue());
  }

  private static Unit spawnUnit(Player owner, UnitType type, int gridX, int gridY) {
    return new Unit(
        owner,
        UnitGrid.coordinateFromGrid(gridX),
        UnitGrid.coordinateFromGrid(gridY),
        null,
        owner.getRaceInfo().getUnitTemplate(type));
  }

  private static World createCleanWorld() {
    LandscapeConfigHelper helper = new LandscapeConfigHelper();
    return helper.world;
  }

  private static final class LandscapeConfigHelper {
    final World world;

    LandscapeConfigHelper() {
      int gridSize = 64;
      WorldParameters worldParams = new WorldParameters("1", 5, 20);
      List<PlayerInfo> playerInfos =
          List.of(new PlayerInfo(0, Race.VIKINGS, "VikingPlayer"), new PlayerInfo(1, Race.NATIVES, "NativePlayer"));
      Color.Linear[] playerColors =
          new Color.Linear[] {new Color.Linear(1f, 0f, 0f, 1f), new Color.Linear(0f, 0f, 1f, 1f)};
      this.world = new World(worldParams, FlatLandscape.create(gridSize), PlayerConfig.from(playerInfos, playerColors));
    }
  }
}
