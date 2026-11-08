package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.SpawnLocation;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.util.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying player starting entity initialization and base placement.
 */
class PlayerStartingEntitiesTest {

    private World world;
    private Player player;

    @BeforeEach
    void setUp() {
        int gridSize = 128;
        Landscape landscape = FlatLandscape.create(gridSize);
        WorldParameters worldParams = new WorldParameters(0, "1", 5, 20);

        List<PlayerInfo> playerInfos = List.of(
                new PlayerInfo(0, Race.VIKINGS, "VikingPlayer"),
                new PlayerInfo(1, Race.NATIVES, "NativePlayer")
        );

        Color.Linear[] playerColors = new Color.Linear[]{
                new Color.Linear(1f, 0f, 0f, 1f),
                new Color.Linear(0f, 0f, 1f, 1f)
        };

        world = new World(
                worldParams,
                landscape,
                PlayerConfig.from(playerInfos, playerColors)
        );

        player = world.getPlayers().getFirst();
    }

    @Test
    void testInitializePlayerBaseSpawnsStructuresAndUnits() {
        UnitInfo unitInfo = new UnitInfo(true, true, 1, true, 3, 2, 1, 0);

        player.initializeStartingBase(unitInfo);

        List<Building> buildings = player.getUnits().getSet().stream()
                .filter(s -> s instanceof Building)
                .map(s -> (Building) s)
                .toList();
        assertTrue(buildings.stream().anyMatch(b -> b.getTemplate().getBuildingType() == BuildingType.QUARTERS));
        assertTrue(buildings.stream().anyMatch(b -> b.getTemplate().getBuildingType() == BuildingType.ARMORY));
        assertEquals(1, buildings.stream().filter(b -> b.getTemplate().getBuildingType() == BuildingType.TOWER)
                .count());

        assertTrue(player.getChieftain().isPresent(), "Chieftain should be spawned and active");

        List<Selectable<?>> units = player.getUnits().getSet().stream().toList();
        assertEquals(3, units.stream().filter(u -> u instanceof Unit unit
                && unit.getTemplate().getUnitType() == UnitType.PEON).count());
        assertEquals(2, units.stream().filter(u -> u instanceof Unit unit
                && unit.getTemplate().getUnitType() == UnitType.WARRIOR_ROCK).count());
        assertEquals(1, units.stream().filter(u -> u instanceof Unit unit
                && unit.getTemplate().getUnitType() == UnitType.WARRIOR_IRON).count());
    }

    @Test
    void testInitializePlayerArmySpawnsUnitsAtLocations() {
        UnitInfo unitInfo = new UnitInfo(false, false, 0, true, 2, 1, 0, 0);
        List<SpawnLocation> locations = List.of(
                new SpawnLocation(5f, 5f),
                new SpawnLocation(7f, 7f),
                new SpawnLocation(9f, 9f),
                new SpawnLocation(11f, 11f)
        );

        player.initializeStartingArmy(locations, unitInfo, 2, "TestChief");

        assertEquals(2, player.getPreferredGamespeed());
        assertTrue(player.getChieftain().isPresent());
        assertEquals("TestChief", player.getChieftain().get().getName());

        List<Selectable<?>> units = player.getUnits().getSet().stream().toList();
        assertEquals(2, units.stream().filter(u -> u instanceof Unit unit
                && unit.getTemplate().getUnitType() == UnitType.PEON).count());
        assertEquals(1, units.stream().filter(u -> u instanceof Unit unit
                && unit.getTemplate().getUnitType() == UnitType.WARRIOR_ROCK).count());
    }

    @Test
    void testInitPlayerWithAiNormalInitializesBase() {
        UnitInfo unitInfo = new UnitInfo(true, false, 0, false, 3, 0, 0, 0);

        player = new Player(world, new PlayerConfig(player.getPlayerInfo(), player.getColor(),
                PlayerControllerType.AI_STANDARD, unitInfo),
                new SpawnLocation(10f, 10f));
        player.initializeStartingEntities(List.of(), 2);

        assertEquals(1, player.getBuildingCountContainer().getNumSupplies());
        assertEquals(3, player.getUnits().getSet().stream().filter(u -> u instanceof Unit).count());
    }

    @Test
    void testInitPlayerWithTowerTutorialSpawnsNothing() {
        UnitInfo unitInfo = new UnitInfo(false, false, 0, false, 0, 0, 0, 0);

        player = new Player(world, new PlayerConfig(player.getPlayerInfo(), player.getColor(),
                PlayerControllerType.AI_TOWER_TUTORIAL, unitInfo),
                new SpawnLocation(10f, 10f));
        player.initializeStartingEntities(List.of(), 2);

        assertEquals(0, player.getBuildingCountContainer().getNumSupplies());
        assertEquals(0, player.getUnits().size());
    }

    @Test
    void testInitPlayerWithChieftainTutorialSpawnsThreePeons() {
        UnitInfo unitInfo = new UnitInfo(false, false, 0, false, 0, 0, 0, 0);

        player = new Player(world, new PlayerConfig(player.getPlayerInfo(), player.getColor(),
                PlayerControllerType.AI_CHIEFTAIN_TUTORIAL, unitInfo),
                new SpawnLocation(10f, 10f));
        player.initializeStartingEntities(List.of(), 2);

        assertEquals(0, player.getBuildingCountContainer().getNumSupplies());
        assertEquals(3, player.getUnits().getSet().stream().filter(u -> u instanceof Unit).count());
        assertTrue(player.getUnits().getSet().stream().allMatch(s -> s instanceof Unit u
                && u.getTemplate().getUnitType() == UnitType.PEON));
    }

    @Test
    void testInitPlayerWithHumanSlotInitializesArmy() {
        UnitInfo unitInfo = new UnitInfo(false, false, 0, false, 2, 0, 0, 0);
        List<SpawnLocation> locations = List.of(
                new SpawnLocation(5f, 5f),
                new SpawnLocation(7f, 7f)
        );

        player = new Player(world, new PlayerConfig(player.getPlayerInfo(), player.getColor(),
                PlayerControllerType.HUMAN, unitInfo),
                new SpawnLocation(10f, 10f));
        player.initializeStartingEntities(locations, 2);

        assertEquals(2, player.getPreferredGamespeed());
        assertEquals(2, player.getUnits().getSet().stream().filter(u -> u instanceof Unit).count());
    }

    @Test
    void testInitializePlayerArmyThrowsWhenStartingLocationsInsufficientForChieftain() {
        UnitInfo unitInfo = new UnitInfo(false, false, 0, true, 2, 0, 0, 0);
        List<SpawnLocation> locations = List.of(
                new SpawnLocation(5f, 5f),
                new SpawnLocation(7f, 7f)
        );

        assertThrows(IndexOutOfBoundsException.class, () -> player.initializeStartingArmy(locations,
                unitInfo, 2, null));
    }

    @Test
    void testWorldInitializesPlayerEntitiesViaConfig() {
        int gridSize = 128;
        Landscape landscape = FlatLandscape.create(gridSize);
        WorldParameters worldParams = new WorldParameters(0, "1", 5, 20);
        UnitInfo unitInfo = new UnitInfo(true, true, 1, true, 3, 2, 1, 0);

        List<PlayerConfig> configs = List.of(
                new PlayerConfig(
                        new PlayerInfo(0, Race.VIKINGS, "VikingPlayer"),
                        new Color.Linear(1f, 0f, 0f, 1f),
                        null,
                        PlayerControllerType.AI_STANDARD,
                        unitInfo,
                        null
                )
        );

        World world = new World(
                worldParams,
                landscape,
                configs
        );

        Player p = world.getPlayers().getFirst();
        assertTrue(p.getChieftain().isPresent(), "Chieftain should be initialized during world construction");
        assertEquals(3, p.getUnits().getSet().stream().filter(u -> u instanceof Unit unit
                && unit.getTemplate().getUnitType() == UnitType.PEON).count());
    }

    @Test
    void testPlayerControllerTypeFromSlotMapping() {
        assertEquals(PlayerControllerType.HUMAN, PlayerControllerType.fromSlot(null));

        PlayerSlot humanSlot = new PlayerSlot(0);
        humanSlot.setType(PlayerSlot.HUMAN);
        assertEquals(PlayerControllerType.HUMAN, PlayerControllerType.fromSlot(humanSlot));

        PlayerSlot standardAiSlot = new PlayerSlot(1);
        standardAiSlot.setType(PlayerSlot.AI);
        standardAiSlot.setAIDifficulty(PlayerSlot.AI_NORMAL);
        assertEquals(PlayerControllerType.AI_STANDARD, PlayerControllerType.fromSlot(standardAiSlot));

        PlayerSlot towerAiSlot = new PlayerSlot(2);
        towerAiSlot.setType(PlayerSlot.AI);
        towerAiSlot.setAIDifficulty(PlayerSlot.AI_TOWER_TUTORIAL);
        assertEquals(PlayerControllerType.AI_TOWER_TUTORIAL, PlayerControllerType.fromSlot(towerAiSlot));

        PlayerSlot chieftainAiSlot = new PlayerSlot(3);
        chieftainAiSlot.setType(PlayerSlot.AI);
        chieftainAiSlot.setAIDifficulty(PlayerSlot.AI_CHIEFTAIN_TUTORIAL);
        assertEquals(PlayerControllerType.AI_CHIEFTAIN_TUTORIAL, PlayerControllerType.fromSlot(chieftainAiSlot));
    }
}
