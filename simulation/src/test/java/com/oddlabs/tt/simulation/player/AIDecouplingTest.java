package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.LandscapeBoundsProvider;
import com.oddlabs.tt.simulation.landscape.LandscapeData;
import com.oddlabs.tt.simulation.landscape.LandscapeGeometry;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.Difficulty;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RaceData;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Target;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying that AI controllers dispatch all commands through {@link PlayerInterface}
 * and do not register with the world's real-time animation manager upon construction.
 */
class AIDecouplingTest {

    private static final class RecordingPlayerInterface implements PlayerInterface {
        final List<String> calls = new ArrayList<>();

        @Override
        public void deployUnits(Building building, DeployType type, int num_units) {
            calls.add("deployUnits:" + type + ":" + num_units);
        }

        @Override
        public void createHarvesters(Building building, int num_tree, int num_rock, int num_iron, int num_rubber) {
            calls.add("createHarvesters");
        }

        @Override
        public void buildRockWeapons(Building building, int num_weapons, boolean infinite) {
            calls.add("buildRockWeapons:" + infinite);
        }

        @Override
        public void buildIronWeapons(Building building, int num_weapons, boolean infinite) {
            calls.add("buildIronWeapons:" + infinite);
        }

        @Override
        public void buildRubberWeapons(Building building, int num_weapons, boolean infinite) {
            calls.add("buildRubberWeapons:" + infinite);
        }

        @Override
        public void doMagic(Unit chieftain, MagicType magic) {
            calls.add("doMagic:" + magic);
        }

        @Override
        public void exitTower(Building building) {
            calls.add("exitTower");
        }

        @Override
        public void trainChieftain(Building building, boolean start) {
            calls.add("trainChieftain:" + start);
        }

        @Override
        public void placeBuilding(Selectable<?>[] selection, BuildingType template_type, int placing_grid_x,
                int placing_grid_y) {
            calls.add("placeBuilding:" + template_type);
        }

        @Override
        public void setRallyPoint(Building building, Target target) {
            calls.add("setRallyPointTarget");
        }

        @Override
        public void setTarget(Selectable<?>[] selection, Target target, Action action, boolean aggressive) {
            calls.add("setTarget:" + action);
        }

        @Override
        public void setRallyPoint(Building building, int grid_x, int grid_y) {
            calls.add("setRallyPointCoords");
        }

        @Override
        public void setLandscapeTarget(Selectable<?>[] selection, int grid_x, int grid_y, Action action,
                boolean aggressive) {
            calls.add("setLandscapeTarget:" + action);
        }

        @Override
        public void setPreferredGamespeed(int speed) {
            calls.add("setPreferredGamespeed:" + speed);
        }

        @Override
        public void changePreferredGamespeed(int delta) {
            calls.add("changePreferredGamespeed:" + delta);
        }
    }

    private record TestLandscapeData(int size,
                                     float[] heightmap,
                                     boolean[][] accessGrid,
                                     byte[][] buildGrid,
                                     float[][] startingLocations) implements LandscapeData {

        static TestLandscapeData create(int size) {
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

            float[][] startingLocations = new float[2][40];
            for (int i = 0; i < 20; i++) {
                startingLocations[0][2 * i] = 10.0f + (i % 5) * 1.5f;
                startingLocations[0][2 * i + 1] = 10.0f + (i / 5f) * 1.5f;

                startingLocations[1][2 * i] = 40.0f + (i % 5) * 1.5f;
                startingLocations[1][2 * i + 1] = 40.0f + (i / 5f) * 1.5f;
            }

            return new TestLandscapeData(size, heightmap, accessGrid, buildGrid, startingLocations);
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
        public List<int[]> trees() {
            return List.of();
        }

        @Override
        public List<int[]> palmTrees() {
            return List.of();
        }

        @Override
        public List<int[]> rocks() {
            return List.of();
        }

        @Override
        public List<int[]> iron() {
            return List.of();
        }

        @Override
        public float[][] plants() {
            return new float[0][0];
        }
    }

    private World world;
    private Player player;
    private RecordingPlayerInterface recordingInterface;

    @BeforeEach
    void setUp() {
        int gridSize = 32;
        LandscapeData landscapeData = TestLandscapeData.create(gridSize);
        WorldParameters worldParams = new WorldParameters(0, "1", 5, 20);

        List<PlayerInfo> playerInfos = List.of(
                new PlayerInfo(0, Race.VIKINGS, "VikingAI"),
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
                landscapeData,
                playerInfos,
                playerColors,
                false
        );

        player = world.getPlayers().getFirst();
        recordingInterface = new RecordingPlayerInterface();
    }

    @Test
    void testAiCreationDoesNotRegisterWithRealTimeAnimationManager() {
        AdvancedAI ai = new AdvancedAI(player, recordingInterface, null, Difficulty.NORMAL);
        assertSame(recordingInterface, ai.getPlayerInterface());

        // Advance the world's real-time animation manager by 20 seconds.
        // Because the AI was NOT automatically registered, no decision cycle runs and no commands are dispatched.
        world.getAnimationManagerRealTime().runAnimations(20.0f);
        assertTrue(recordingInterface.calls.isEmpty(),
                "AI should not be triggered by real-time animation manager when not registered");
    }

    @Test
    void testManTowersDispatchesThroughPlayerInterface() {
        AdvancedAI ai = new AdvancedAI(player, recordingInterface, null, Difficulty.NORMAL);

        // Spawn a tower and an idle warrior for the AI player
        Building tower = new Building(player, player.getRaceInfo().getBuildingTemplate(BuildingType.TOWER), 10, 10);
        tower.place();
        tower.repair(1000);

        new Unit(player, 10, 10, null, player.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK), null);

        ai.manTowers(1);

        assertTrue(recordingInterface.calls.contains("setTarget:DEFAULT"),
                "manTowers should dispatch order through playerInterface.setTarget");
    }

    @Test
    void testAttackLandscapeDispatchesThroughPlayerInterface() {
        Unit warrior = new Unit(player, 15, 15, null, player.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK),
                null);
        Target target = world.getUnitGrid().findGridTargets(15, 15, 1, false)[0];

        int ordered = AI.attackLandscape(player, recordingInterface, target, 1);
        assertEquals(1, ordered);
        assertTrue(recordingInterface.calls.contains("setLandscapeTarget:ATTACK"),
                "attackLandscape should dispatch through playerInterface.setLandscapeTarget");
    }

    @Test
    void testVikingChieftainMagicDispatchesThroughPlayerInterface() {
        Unit chieftain = new Unit(player, 12, 12, null, player.getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN), null);
        chieftain.maxMagicEnergy(MagicType.STUN);

        // Spawn 5 enemy units close to the chieftain to meet NUM_UNITS_FOR_STUN threshold
        Player enemy = world.getPlayers().get(1);
        for (int i = 0; i < 5; i++) {
            new Unit(enemy, 12, 12, null, enemy.getRaceInfo().getUnitTemplate(UnitType.PEON), null);
        }

        ChieftainAI chieftainAI = new VikingChieftainAI();
        chieftainAI.decide(chieftain, recordingInterface);

        assertTrue(recordingInterface.calls.contains("doMagic:STUN"),
                "Chieftain magic should dispatch through playerInterface.doMagic");
    }

    @Test
    void testNativeChieftainMagicDispatchesThroughPlayerInterface() {
        Player nativePlayer = world.getPlayers().get(1);
        Unit chieftain = new Unit(nativePlayer, 20, 20, null, nativePlayer.getRaceInfo().getUnitTemplate(
                UnitType.CHIEFTAIN), null);
        chieftain.maxMagicEnergy(MagicType.LIGHTNING_CLOUD);

        // Spawn 2 enemy units close to meet NUM_UNITS_FOR_LIGHTNING
        for (int i = 0; i < 2; i++) {
            new Unit(player, 20, 20, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON), null);
        }

        ChieftainAI chieftainAI = new NativeChieftainAI();
        chieftainAI.decide(chieftain, recordingInterface);

        assertTrue(recordingInterface.calls.contains("doMagic:LIGHTNING_CLOUD"),
                "Native chieftain magic should dispatch through playerInterface.doMagic");
    }
}
