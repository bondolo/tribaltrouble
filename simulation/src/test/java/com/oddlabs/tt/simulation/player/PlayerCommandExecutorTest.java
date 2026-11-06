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
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.DistributableTable;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RaceData;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PlayerCommandExecutor} verifying command validation, entity resolution, and execution.
 */
class PlayerCommandExecutorTest {

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
    private DistributableTable table;
    private Player player0;
    private Player player1;
    private List<String> rejections;

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

        world = World.newWorld(
                raceData,
                boundsProvider,
                noopListener,
                worldParams,
                landscape,
                playerInfos,
                playerColors
        );

        table = world.getDistributableTable();
        player0 = world.getPlayers().get(0);
        player1 = world.getPlayers().get(1);

        UnitInfo unitInfo = new UnitInfo(true, true, 1, true, 3, 2, 1, 0);
        PlayerInitializer.initializePlayerBase(player0, unitInfo);
        PlayerInitializer.initializePlayerBase(player1, unitInfo);

        rejections = new ArrayList<>();
        player0.setCommandRejectedListener((action, entityId, reason) -> rejections.add(action + ":" + reason));
    }

    @Test
    void testSetPreferredGamespeedCommands() {
        PlayerCommandExecutor executor = player0;
        executor.execute(new PlayerCommand.SetPreferredGamespeed(2));
        assertEquals(2, player0.getPreferredGamespeed());

        player0.execute(new PlayerCommand.ChangePreferredGamespeed(1));
        assertEquals(3, player0.getPreferredGamespeed());
    }

    @Test
    void testDeployUnitsValidation() {
        Building quarters = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Building b && b.getTemplate().getBuildingType() == BuildingType.QUARTERS)
                .map(s -> (Building) s)
                .findFirst()
                .orElseThrow();
        int quartersId = table.findName(quarters).orElseThrow();

        // Valid deploy command
        player0.execute(new PlayerCommand.DeployUnits(quartersId, DeployType.PEON, 2));
        assertTrue(rejections.isEmpty());

        // Non-existent building ID
        player0.execute(new PlayerCommand.DeployUnits(99999, DeployType.PEON, 1));
        assertEquals(List.of("deployUnits:ENTITY_NOT_FOUND"), rejections);
        rejections.clear();

        // Building owned by player1 attempted by player0
        Building enemyQuarters = player1.getUnits().getSet().stream()
                .filter(s -> s instanceof Building b && b.getTemplate().getBuildingType() == BuildingType.QUARTERS)
                .map(s -> (Building) s)
                .findFirst()
                .orElseThrow();
        int enemyQuartersId = table.findName(enemyQuarters).orElseThrow();
        player0.execute(new PlayerCommand.DeployUnits(enemyQuartersId, DeployType.PEON, 1));
        assertEquals(List.of("deployUnits:NOT_OWNED"), rejections);
    }

    @Test
    void testPlaceBuildingAndCoordinatesValidation() {
        Unit peon = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Unit u && u.getTemplate().getUnitType() == UnitType.PEON)
                .map(s -> (Unit) s)
                .findFirst()
                .orElseThrow();
        int peonId = table.findName(peon).orElseThrow();

        // Invalid grid coordinate rejection throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                player0.execute(new PlayerCommand.PlaceBuilding(new int[]{peonId}, BuildingType.TOWER, -1, 10)));
        assertThrows(IllegalArgumentException.class, () ->
                player0.execute(new PlayerCommand.PlaceBuilding(new int[]{peonId}, BuildingType.TOWER, 10, 1000)));

        // Valid placement orders peon to target the new building construction site
        player0.execute(new PlayerCommand.PlaceBuilding(new int[]{peonId}, BuildingType.TOWER, 15, 15));
        assertTrue(peon
                .getCurrentController() instanceof com.oddlabs.tt.simulation.behaviour.PlaceBuildingController placeCtrl
                && placeCtrl.getBuilding().getTemplate().getBuildingType() == BuildingType.TOWER);
    }

    @Test
    void testSetLandscapeTargetAndSetTarget() {
        Unit peon = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Unit u && u.getTemplate().getUnitType() == UnitType.PEON)
                .map(s -> (Unit) s)
                .findFirst()
                .orElseThrow();
        int peonId = table.findName(peon).orElseThrow();

        // Landscape move command
        player0.execute(new PlayerCommand.SetLandscapeTarget(new int[]{peonId}, 12, 12, Action.MOVE, false));
        assertTrue(rejections.isEmpty());

        // Entity target command against enemy chieftain
        Unit enemyChieftain = player1.getChieftain().orElseThrow();
        int enemyChiefId = table.findName(enemyChieftain).orElseThrow();

        player0.execute(new PlayerCommand.SetTarget(new int[]{peonId}, enemyChiefId, Action.ATTACK, true));
        assertTrue(rejections.isEmpty());

        // Target non-existent entity
        player0.execute(new PlayerCommand.SetTarget(new int[]{peonId}, 88888, Action.ATTACK, true));
        assertEquals(List.of("setTarget:ENTITY_NOT_FOUND"), rejections);
    }

    @Test
    void testDoMagicValidation() {
        Unit chieftain = player0.getChieftain().orElseThrow();
        int chiefId = table.findName(chieftain).orElseThrow();

        player0.execute(new PlayerCommand.DoMagic(chiefId, MagicType.LIGHTNING_CLOUD));
        assertTrue(rejections.isEmpty());

        // Reject non-existent chieftain
        player0.execute(new PlayerCommand.DoMagic(77777, MagicType.LIGHTNING_CLOUD));
        assertEquals(List.of("doMagic:ENTITY_NOT_FOUND"), rejections);
        rejections.clear();

        // Reject enemy chieftain
        Unit enemyChieftain = player1.getChieftain().orElseThrow();
        int enemyChiefId = table.findName(enemyChieftain).orElseThrow();
        player0.execute(new PlayerCommand.DoMagic(enemyChiefId, MagicType.LIGHTNING_CLOUD));
        assertEquals(List.of("doMagic:NOT_OWNED"), rejections);
    }

    @Test
    void testPlayerActionsExplodedMethodsDirectly() {
        PlayerActions actions = player0;

        Building quarters = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Building b && b.getTemplate().getBuildingType() == BuildingType.QUARTERS)
                .map(s -> (Building) s)
                .findFirst()
                .orElseThrow();
        int quartersId = table.findName(quarters).orElseThrow();

        // Direct exploded method call on PlayerActions
        actions.deployUnits(quartersId, DeployType.PEON, 1);
        assertTrue(rejections.isEmpty());

        actions.deployUnits(99999, DeployType.PEON, 1);
        assertEquals(List.of("deployUnits:ENTITY_NOT_FOUND"), rejections);
        rejections.clear();

        // Direct exploded gamespeed call
        actions.setPreferredGamespeed(4);
        assertEquals(4, player0.getPreferredGamespeed());
        actions.changePreferredGamespeed(-1);
        assertEquals(3, player0.getPreferredGamespeed());

        // Convenience single-unit overload
        Unit peon = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Unit u && u.getTemplate().getUnitType() == UnitType.PEON)
                .map(s -> (Unit) s)
                .findFirst()
                .orElseThrow();
        int peonId = table.findName(peon).orElseThrow();
        actions.setLandscapeTarget(peonId, 14, 14, Action.MOVE, false);
        assertTrue(rejections.isEmpty());

        // Default execute dispatch via PlayerActions interface reference
        actions.execute(new PlayerCommand.DeployUnits(quartersId, DeployType.PEON, 1));
        assertTrue(rejections.isEmpty());
    }

    @Test
    void testArrayBasedRecordEqualityAndHashCode() {
        PlayerCommand.PlaceBuilding pb1 = new PlayerCommand.PlaceBuilding(new int[]{1, 2, 3}, BuildingType.QUARTERS, 10,
                20);
        PlayerCommand.PlaceBuilding pb2 = new PlayerCommand.PlaceBuilding(new int[]{1, 2, 3}, BuildingType.QUARTERS, 10,
                20);
        PlayerCommand.PlaceBuilding pb3 = new PlayerCommand.PlaceBuilding(new int[]{1, 2, 4}, BuildingType.QUARTERS, 10,
                20);

        assertEquals(pb1, pb2);
        assertEquals(pb1.hashCode(), pb2.hashCode());
        assertNotEquals(pb1, pb3);
        assertTrue(pb1.toString().contains("[1, 2, 3]"));

        PlayerCommand.SetTarget st1 = new PlayerCommand.SetTarget(new int[]{5, 6}, 100, Action.ATTACK, true);
        PlayerCommand.SetTarget st2 = new PlayerCommand.SetTarget(new int[]{5, 6}, 100, Action.ATTACK, true);
        PlayerCommand.SetTarget st3 = new PlayerCommand.SetTarget(new int[]{5, 7}, 100, Action.ATTACK, true);

        assertEquals(st1, st2);
        assertEquals(st1.hashCode(), st2.hashCode());
        assertNotEquals(st1, st3);
        assertTrue(st1.toString().contains("[5, 6]"));

        PlayerCommand.SetLandscapeTarget slt1 = new PlayerCommand.SetLandscapeTarget(new int[]{8, 9}, 15, 25,
                Action.MOVE, false);
        PlayerCommand.SetLandscapeTarget slt2 = new PlayerCommand.SetLandscapeTarget(new int[]{8, 9}, 15, 25,
                Action.MOVE, false);
        PlayerCommand.SetLandscapeTarget slt3 = new PlayerCommand.SetLandscapeTarget(new int[]{8, 10}, 15, 25,
                Action.MOVE, false);

        assertEquals(slt1, slt2);
        assertEquals(slt1.hashCode(), slt2.hashCode());
        assertNotEquals(slt1, slt3);
        assertTrue(slt1.toString().contains("[8, 9]"));
    }
}
