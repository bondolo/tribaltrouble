package com.oddlabs.tt.simulation.model;

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
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests filtering and ability detection on {@link Army}.
 */
class ArmyTest {

    private record TestLandscape(int size,
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
                Arrays.fill(row, (byte) 20);
            }

            List<List<SpawnLocation>> startingLocations = List.of(
                    List.of(new SpawnLocation(10.0f, 10.0f)),
                    List.of(new SpawnLocation(20.0f, 20.0f))
            );

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

    private Player player;

    @BeforeEach
    void setUp() {
        int gridSize = 32;
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

        World world = World.newWorld(
                raceData,
                boundsProvider,
                noopListener,
                worldParams,
                landscape,
                playerInfos,
                playerColors,
                false
        );

        player = world.getPlayers().getFirst();
    }

    @Test
    void testEmptyArmy() {
        Army army = new Army();
        assertEquals(0, army.size());
        assertFalse(army.containsAbility(Abilities.BUILD));
        assertFalse(army.containsAbility(Abilities.ATTACK));

        Selectable<?>[] filtered = army.filter(Abilities.BUILD);
        assertEquals(0, filtered.length);
        assertSame(filtered, army.filter(Abilities.ATTACK), "Empty filter should return shared empty array");
    }

    @Test
    void testFilteringAndAbilityChecks() {
        Army army = new Army();

        Unit peon = new Unit(player, 10, 10, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
        Unit warrior = new Unit(player, 11, 11, null, player.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));

        army.add(peon);
        army.add(warrior);

        assertEquals(2, army.size());
        assertTrue(army.contains(peon));
        assertTrue(army.contains(warrior));

        assertTrue(army.containsAbility(Abilities.BUILD));
        assertTrue(army.containsAbility(Abilities.ATTACK));
        assertFalse(army.containsAbility(Abilities.MAGIC));

        Selectable<?>[] builders = army.filter(Abilities.BUILD);
        assertEquals(1, builders.length);
        assertSame(peon, builders[0]);

        Selectable<?>[] throwers = army.filter(Abilities.THROW);
        assertEquals(1, throwers.length);
        assertSame(warrior, throwers[0]);

        Selectable<?>[] attackers = army.filter(Abilities.ATTACK);
        assertEquals(2, attackers.length);

        Selectable<?>[] all = army.filter(Abilities.NONE);
        assertEquals(2, all.length);

        Selectable<?>[] magic = army.filter(Abilities.MAGIC);
        assertEquals(0, magic.length);
    }
}
