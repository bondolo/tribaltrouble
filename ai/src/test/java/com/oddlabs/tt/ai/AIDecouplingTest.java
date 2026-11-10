package com.oddlabs.tt.ai;

import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.PlayerInterface;

import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.Difficulty;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.model.WeaponTier;
import com.oddlabs.util.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        public void buildWeapons(Building building, WeaponTier tier, int num_weapons, boolean infinite) {
            calls.add("buildWeapons:" + tier + ":" + infinite);
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
        public void placeBuilding(List<? extends Selectable<?>> selection, BuildingType template_type,
                int placing_grid_x,
                int placing_grid_y) {
            calls.add("placeBuilding:" + template_type);
        }

        @Override
        public void setRallyPoint(Building building, Target target) {
            calls.add("setRallyPointTarget");
        }

        @Override
        public void setTarget(List<? extends Selectable<?>> selection, Target target, Action action,
                boolean aggressive) {
            calls.add("setTarget:" + action);
        }

        @Override
        public void setTarget(Selectable<?> unit, Target target, Action action, boolean aggressive) {
            calls.add("setTarget:" + action);
        }

        @Override
        public void setRallyPoint(Building building, int grid_x, int grid_y) {
            calls.add("setRallyPointCoords");
        }

        @Override
        public void setLandscapeTarget(List<? extends Selectable<?>> selection, int grid_x, int grid_y, Action action,
                boolean aggressive) {
            calls.add("setLandscapeTarget:" + action);
        }

        @Override
        public void setLandscapeTarget(Selectable<?> unit, int grid_x, int grid_y, Action action,
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

    private World world;
    private Player player;
    private RecordingPlayerInterface recordingInterface;

    @BeforeEach
    void setUp() {
        int gridSize = 32;
        Landscape landscape = FlatLandscape.create(gridSize,
                FlatLandscape.generateStartingLocations(2, 20, 10.0f, 40.0f));
        WorldParameters worldParams = new WorldParameters(0, "1", 5, 20);

        List<PlayerInfo> playerInfos = List.of(
                new PlayerInfo(0, Race.VIKINGS, "VikingAI"),
                new PlayerInfo(1, Race.NATIVES, "NativePlayer")
        );

        Color.Linear[] playerColors = new Color.Linear[]{
                new Color.Linear(1f, 0f, 0f, 1f),
                new Color.Linear(0f, 0f, 1f, 1f)
        };

        recordingInterface = new RecordingPlayerInterface();
        List<PlayerConfig> playerConfigs = List.of(
                new PlayerConfig(playerInfos.get(0), playerColors[0], recordingInterface),
                new PlayerConfig(playerInfos.get(1), playerColors[1])
        );
        world = new World(worldParams, landscape, playerConfigs);

        player = world.getPlayers().getFirst();
    }

    @Test
    void testAiCreationIsSideEffectFree() {
        int initialUnitCount = player.getUnits().size();
        int initialBuildingCount = player.getBuildingCountContainer().getNumSupplies();

        AdvancedAI ai = new AdvancedAI(player, Difficulty.NORMAL);
        assertSame(recordingInterface, player.getPlayerInterface());

        assertEquals(initialUnitCount, player.getUnits().size(),
                "Instantiating AI controller must not alter player unit count");
        assertEquals(initialBuildingCount, player.getBuildingCountContainer().getNumSupplies(),
                "Instantiating AI controller must not alter player building count");
    }

    @Test
    void testAiCreationDoesNotRegisterWithRealTimeAnimationManager() {
        AdvancedAI ai = new AdvancedAI(player, Difficulty.NORMAL);
        assertSame(recordingInterface, player.getPlayerInterface());

        // Advance the world's real-time animation manager by 20 seconds.
        // Because the AI was NOT automatically registered, no decision cycle runs and no commands are dispatched.
        world.getAnimationManagerRealTime().runAnimations(20.0f);
        assertTrue(recordingInterface.calls.isEmpty(),
                "AI should not be triggered by real-time animation manager when not registered");
    }

    @Test
    void testManTowersDispatchesThroughPlayerInterface() {
        AdvancedAI ai = new AdvancedAI(player, Difficulty.NORMAL);

        // Spawn a tower and an idle warrior for the AI player
        Building tower = new Building(player, player.getRaceInfo().getBuildingTemplate(BuildingType.TOWER), 10, 10);
        tower.place();
        tower.repair(1000);

        new Unit(player, 10, 10, null, player.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));

        ai.manTowers(1);

        assertTrue(recordingInterface.calls.contains("setTarget:DEFAULT"),
                "manTowers should dispatch order through playerInterface.setTarget");
    }

    @Test
    void testAttackLandscapeDispatchesThroughPlayerInterface() {
        Unit warrior = new Unit(player, 15, 15, null, player.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        Target target = java.util.Objects.requireNonNull(world.getUnitGrid().findGridTarget(15, 15, false));

        int ordered = AI.attackLandscape(player, target, 1);
        assertEquals(1, ordered);
        assertTrue(recordingInterface.calls.contains("setLandscapeTarget:ATTACK"),
                "attackLandscape should dispatch through playerInterface.setLandscapeTarget");
    }

    @Test
    void testVikingChieftainMagicDispatchesThroughPlayerInterface() {
        Unit chieftain = new Unit(player, 12, 12, null, player.getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN));
        chieftain.maxMagicEnergy(MagicType.STUN);

        // Spawn 5 enemy units close to the chieftain to meet NUM_UNITS_FOR_STUN threshold
        Player enemy = world.getPlayers().get(1);
        for (int i = 0; i < 5; i++) {
            new Unit(enemy, 12, 12, null, enemy.getRaceInfo().getUnitTemplate(UnitType.PEON));
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
                UnitType.CHIEFTAIN));
        chieftain.maxMagicEnergy(MagicType.LIGHTNING_CLOUD);

        // Spawn 2 enemy units close to meet NUM_UNITS_FOR_LIGHTNING
        for (int i = 0; i < 2; i++) {
            new Unit(player, 20, 20, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
        }

        ChieftainAI chieftainAI = new NativeChieftainAI();
        chieftainAI.decide(chieftain, recordingInterface);

        assertTrue(recordingInterface.calls.contains("doMagic:LIGHTNING_CLOUD"),
                "Native chieftain magic should dispatch through playerInterface.doMagic");
    }

    @Test
    void testChieftainAiForRace() {
        assertInstanceOf(VikingChieftainAI.class, ChieftainAI.forRace(Race.VIKINGS));
        assertInstanceOf(NativeChieftainAI.class, ChieftainAI.forRace(Race.NATIVES));
        assertSame(ChieftainAI.forRace(Race.VIKINGS), ChieftainAI.forRace(Race.VIKINGS));
        assertSame(ChieftainAI.forRace(Race.NATIVES), ChieftainAI.forRace(Race.NATIVES));
    }

    @Test
    void testPassiveAiWanderDispatchesThroughPlayerInterfaceSetLandscapeTarget() {
        PassiveAI ai = new PassiveAI(player, true);
        new Unit(player, 10, 10, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));

        // Advance ticks until random decision triggers wandering (decision cycle is every ~2-4s with 20% probability)
        for (int i = 0; i < 2000 && recordingInterface.calls.isEmpty(); i++) {
            ai.tick();
        }

        assertTrue(recordingInterface.calls.stream().anyMatch(c -> c.startsWith("setLandscapeTarget:")),
                "PassiveAI with walk_around=true should dispatch roaming target through setLandscapeTarget");
        assertFalse(recordingInterface.calls.stream().anyMatch(c -> c.startsWith("setTarget:")),
                "PassiveAI roaming must not dispatch unregistered coordinates through setTarget");
    }

    @Test
    void testPlayerEncapsulatesImmutablePlayerInterface() {
        Player defaultPlayer = world.getPlayers().get(1);
        assertNotNull(defaultPlayer.getPlayerInterface());

        RecordingPlayerInterface custom = new RecordingPlayerInterface();
        Player customPlayer = new Player(world, new PlayerConfig(defaultPlayer.getPlayerInfo(), defaultPlayer
                .getColor(), custom));
        assertSame(custom, customPlayer.getPlayerInterface());

        customPlayer.getPlayerInterface().deployUnits(null, DeployType.PEON, 1);
        assertTrue(custom.calls.contains("deployUnits:PEON:1"));
    }

    @Test
    void testReclassifyWithArmoryDispatchesWeaponsThroughPlayerInterface() {
        AdvancedAI ai = new AdvancedAI(player, Difficulty.NORMAL);

        Building armory = new Building(player, player.getRaceInfo().getBuildingTemplate(BuildingType.ARMORY), 10, 10);
        armory.place();
        armory.repair(1000);

        recordingInterface.calls.clear();
        ai.manTowers(0);

        assertTrue(recordingInterface.calls.contains("buildWeapons:ROCK:true"),
                "reclassify should dispatch rock weapon orders through playerInterface");
        assertTrue(recordingInterface.calls.contains("buildWeapons:IRON:true"),
                "reclassify should dispatch iron weapon orders through playerInterface");
        assertTrue(recordingInterface.calls.contains("buildWeapons:RUBBER:true"),
                "reclassify should dispatch rubber weapon orders through playerInterface");
    }
}
