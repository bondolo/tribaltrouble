package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.LandscapeBoundsProvider;
import com.oddlabs.tt.simulation.landscape.LandscapeGeometry;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.PlantLocation;
import com.oddlabs.tt.simulation.landscape.SpawnLocation;
import com.oddlabs.tt.simulation.landscape.SupplyPlacement;
import com.oddlabs.tt.simulation.landscape.TreePlacement;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.Difficulty;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RaceData;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.util.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying deterministic world player initialization via {@link PlayerInitializer}
 * and side-effect-free controller attachment.
 */
class PlayerInitializerTest {

    private record TestLandscape(
                                 int size,
                                 float[] heightmap,
                                 boolean[][] accessGrid,
                                 byte[][] buildGrid,
                                 List<List<SpawnLocation>> startingLocations) implements Landscape {

        static TestLandscape create(int size) {
            float[] heightmap = new float[size * size];
            Arrays.fill(heightmap, 5.0f);

            boolean[][] accessGrid = new boolean[size][size];
            for (boolean[] row : accessGrid) {
                Arrays.fill(row, true);
            }

            byte[][] buildGrid = new byte[size][size];
            for (byte[] row : buildGrid) {
                Arrays.fill(row, (byte) 10);
            }

            List<List<SpawnLocation>> startingLocations = new ArrayList<>();
            for (int p = 0; p < 2; p++) {
                List<SpawnLocation> playerLocations = new ArrayList<>();
                for (int i = 0; i < 20; i++) {
                    float baseX = (p == 0) ? 10.0f : 20.0f;
                    float baseY = (p == 0) ? 10.0f : 20.0f;
                    playerLocations.add(new SpawnLocation(baseX + (i % 5) * 1.5f, baseY + (i / 5f) * 1.5f));
                }
                startingLocations.add(playerLocations);
            }

            return new TestLandscape(size, heightmap, accessGrid, buildGrid, startingLocations);
        }

        @Override
        public IslandConfig config() {
            return new IslandConfig(Terrain.NATIVE, metersPerWorld(), 0.5f, 0.5f, 0.5f, 42);
        }

        @Override
        public Terrain terrain() {
            return Terrain.NATIVE;
        }

        @Override
        public int metersPerWorld() {
            return size * HeightMap.METERS_PER_UNIT_GRID;
        }

        @Override
        public float seaLevelMeters() {
            return 0.0f;
        }

        @Override
        public List<TreePlacement> trees() {
            return List.of();
        }

        @Override
        public List<TreePlacement> palmTrees() {
            return List.of();
        }

        @Override
        public List<SupplyPlacement> rocks() {
            return List.of();
        }

        @Override
        public List<SupplyPlacement> iron() {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<PlantLocation>[] plants() {
            return new ArrayList[0];
        }
    }

    private World world;
    private Player player;

    @BeforeEach
    void setUp() {
        int gridSize = 128;
        Landscape landscape = TestLandscape.create(gridSize);
        WorldParameters worldParams = new WorldParameters(0, "1", 5, 20);

        List<PlayerInfo> playerInfos = List.of(
                new PlayerInfo(0, Race.VIKINGS, "VikingPlayer"),
                new PlayerInfo(1, Race.NATIVES, "NativePlayer")
        );

        Color.Linear[] playerColors = new Color.Linear[]{
                new Color.Linear(1f, 0f, 0f, 1f),
                new Color.Linear(0f, 0f, 1f, 1f)
        };

        LandscapeBoundsProvider boundsProvider = new LandscapeGeometry();
        RaceData raceData = new RaceData();
        NotificationListener noopListener = new NotificationListener() {
        };

        world = World.newWorld(
                raceData,
                boundsProvider,
                noopListener,
                worldParams,
                landscape,
                playerInfos,
                playerColors
        );

        player = world.getPlayers().getFirst();
    }

    @Test
    void testInitializePlayerBaseSpawnsStructuresAndUnits() {
        UnitInfo unitInfo = new UnitInfo(true, true, 1, true, 3, 2, 1, 0);

        PlayerInitializer.initializePlayerBase(player, unitInfo);

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

        PlayerInitializer.initializePlayerArmy(player, locations, unitInfo, 2, "TestChief");

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
    void testAiCreationIsSideEffectFree() {
        UnitInfo unitInfo = new UnitInfo(false, false, 0, false, 2, 0, 0, 0);
        List<SpawnLocation> locations = List.of(
                new SpawnLocation(5f, 5f),
                new SpawnLocation(7f, 7f)
        );
        PlayerInitializer.initializePlayerArmy(player, locations, unitInfo, 0, null);

        int initialUnitCount = player.getUnits().size();
        int initialBuildingCount = player.getBuildingCountContainer().getNumSupplies();

        AdvancedAI ai = new AdvancedAI(player, player, Difficulty.NORMAL);
        assertNotNull(ai);

        assertEquals(initialUnitCount, player.getUnits().size(),
                "Instantiating AI controller must not alter player unit count");
        assertEquals(initialBuildingCount, player.getBuildingCountContainer().getNumSupplies(),
                "Instantiating AI controller must not alter player building count");
    }

    @Test
    void testInitPlayerWithAiNormalInitializesBase() {
        PlayerSlot slot = new PlayerSlot(0);
        slot.setType(PlayerSlot.AI);
        slot.setAIDifficulty(PlayerSlot.AI_NORMAL);
        UnitInfo unitInfo = new UnitInfo(true, false, 0, false, 3, 0, 0, 0);

        PlayerInitializer.initPlayer(player, slot, unitInfo, List.of(), 2, null);

        assertEquals(1, player.getBuildingCountContainer().getNumSupplies());
        assertEquals(3, player.getUnits().getSet().stream().filter(u -> u instanceof Unit).count());
    }

    @Test
    void testInitPlayerWithTowerTutorialSpawnsNothing() {
        PlayerSlot slot = new PlayerSlot(0);
        slot.setType(PlayerSlot.AI);
        slot.setAIDifficulty(PlayerSlot.AI_TOWER_TUTORIAL);
        UnitInfo unitInfo = new UnitInfo(false, false, 0, false, 0, 0, 0, 0);

        PlayerInitializer.initPlayer(player, slot, unitInfo, List.of(), 2, null);

        assertEquals(0, player.getBuildingCountContainer().getNumSupplies());
        assertEquals(0, player.getUnits().size());
    }

    @Test
    void testInitPlayerWithChieftainTutorialSpawnsThreePeons() {
        PlayerSlot slot = new PlayerSlot(0);
        slot.setType(PlayerSlot.AI);
        slot.setAIDifficulty(PlayerSlot.AI_CHIEFTAIN_TUTORIAL);
        UnitInfo unitInfo = new UnitInfo(false, false, 0, false, 0, 0, 0, 0);

        PlayerInitializer.initPlayer(player, slot, unitInfo, List.of(), 2, null);

        assertEquals(0, player.getBuildingCountContainer().getNumSupplies());
        assertEquals(3, player.getUnits().getSet().stream().filter(u -> u instanceof Unit).count());
        assertTrue(player.getUnits().getSet().stream().allMatch(s -> s instanceof Unit u
                && u.getTemplate().getUnitType() == UnitType.PEON));
    }

    @Test
    void testInitPlayerWithHumanSlotInitializesArmy() {
        PlayerSlot slot = new PlayerSlot(0);
        slot.setType(PlayerSlot.HUMAN);
        UnitInfo unitInfo = new UnitInfo(false, false, 0, false, 2, 0, 0, 0);
        List<SpawnLocation> locations = List.of(
                new SpawnLocation(5f, 5f),
                new SpawnLocation(7f, 7f)
        );

        PlayerInitializer.initPlayer(player, slot, unitInfo, locations, 2, null);

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

        assertThrows(IndexOutOfBoundsException.class, () -> PlayerInitializer.initializePlayerArmy(player, locations,
                unitInfo, 2, null));
    }
}
