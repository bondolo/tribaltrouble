package com.oddlabs.tt.net;

import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.ARMIEventWriter;
import com.oddlabs.net.ARMIInterfaceMethods;
import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.DistributableTable;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.model.WeaponTier;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.PlayerInterface;
import com.oddlabs.tt.simulation.player.UnitInfo;
import com.oddlabs.util.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// Unit tests for {@link GameArgumentWriter} and {@link GameArgumentReader} verifying ARMI proxy serialization.
final class GameArgumentSerializationTest {

    private World world;
    private DistributableTable table;
    private Player player0;
    private Unit peon;

    @BeforeEach
    void setUp() {
        int gridSize = 64;
        Landscape landscape = FlatLandscape.create(gridSize,
                FlatLandscape.generateStartingLocations(2, 20, 25.0f, 40.0f));
        WorldParameters worldParams = new WorldParameters("1", 5, 20);

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

        table = world.getDistributableTable();
        player0 = world.getPlayers().getFirst();

        UnitInfo unitInfo = new UnitInfo(true, true, 1, true, 3, 2, 1, 0);
        player0.initializeStartingBase(unitInfo);

        peon = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Unit u && u.getTemplate().getUnitType() == UnitType.PEON)
                .map(s -> (Unit) s)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void testPlaceBuildingProxySerializationAndExecution() throws Exception {
        GameArgumentWriter writer = new GameArgumentWriter(table);
        GameArgumentReader reader = new GameArgumentReader(table);
        ARMIInterfaceMethods methods = new ARMIInterfaceMethods(PlayerInterface.class);

        AtomicReference<ARMIEvent> capturedEvent = new AtomicReference<>();
        ARMIEventWriter broker = capturedEvent::set;

        PlayerInterface proxy = (PlayerInterface) ARMIEvent.createProxy(broker, writer, PlayerInterface.class);

        // Call placeBuilding with List<? extends Selectable<?>> via ARMI proxy
        proxy.placeBuilding(List.of(peon), BuildingType.QUARTERS, 12, 14);

        ARMIEvent event = capturedEvent.get();
        assertNotNull(event);

        // Execute against recording mock to verify deserialization
        RecordingPlayerInterface recorder = new RecordingPlayerInterface();
        event.execute(methods, reader, recorder);

        assertEquals(1, recorder.placedBuildings.size());
        RecordedPlaceBuilding call = recorder.placedBuildings.getFirst();
        assertEquals(List.of(peon), call.selection());
        assertEquals(BuildingType.QUARTERS, call.templateType());
        assertEquals(12, call.gridX());
        assertEquals(14, call.gridY());
    }

    @Test
    void testSetTargetAndLandscapeTargetProxySerialization() throws Exception {
        GameArgumentWriter writer = new GameArgumentWriter(table);
        GameArgumentReader reader = new GameArgumentReader(table);
        ARMIInterfaceMethods methods = new ARMIInterfaceMethods(PlayerInterface.class);

        AtomicReference<ARMIEvent> capturedEvent = new AtomicReference<>();
        ARMIEventWriter broker = capturedEvent::set;

        PlayerInterface proxy = (PlayerInterface) ARMIEvent.createProxy(broker, writer, PlayerInterface.class);

        // Call setLandscapeTarget
        proxy.setLandscapeTarget(List.of(peon), 20, 25, Action.MOVE, false);
        ARMIEvent moveEvent = capturedEvent.get();
        assertNotNull(moveEvent);

        RecordingPlayerInterface recorder = new RecordingPlayerInterface();
        moveEvent.execute(methods, reader, recorder);

        assertEquals(1, recorder.landscapeTargets.size());
        RecordedLandscapeTarget moveCall = recorder.landscapeTargets.getFirst();
        assertEquals(List.of(peon), moveCall.selection());
        assertEquals(20, moveCall.gridX());
        assertEquals(25, moveCall.gridY());
        assertEquals(Action.MOVE, moveCall.action());
        assertEquals(false, moveCall.aggressive());

        // Call setTarget
        proxy.setTarget(List.of(peon), peon, Action.DEFAULT, true);
        ARMIEvent targetEvent = capturedEvent.get();
        assertNotNull(targetEvent);

        targetEvent.execute(methods, reader, recorder);
        assertEquals(1, recorder.entityTargets.size());
        RecordedEntityTarget targetCall = recorder.entityTargets.getFirst();
        assertEquals(List.of(peon), targetCall.selection());
        assertEquals(peon, targetCall.target());
        assertEquals(Action.DEFAULT, targetCall.action());
        assertEquals(true, targetCall.aggressive());
    }

    private record RecordedPlaceBuilding(List<? extends Selectable<?>> selection, BuildingType templateType,
                                         int gridX, int gridY) {
    }

    private record RecordedLandscapeTarget(List<? extends Selectable<?>> selection, int gridX, int gridY,
                                           Action action, boolean aggressive) {
    }

    private record RecordedEntityTarget(List<? extends Selectable<?>> selection, Target target,
                                        Action action, boolean aggressive) {
    }

    private static final class RecordingPlayerInterface implements PlayerInterface {
        final List<RecordedPlaceBuilding> placedBuildings = new ArrayList<>();
        final List<RecordedLandscapeTarget> landscapeTargets = new ArrayList<>();
        final List<RecordedEntityTarget> entityTargets = new ArrayList<>();

        @Override
        public void deployUnits(Building building, DeployType type, int num_units) {
        }

        @Override
        public void createHarvesters(Building building, int num_tree, int num_rock, int num_iron, int num_rubber) {
        }

        @Override
        public void buildWeapons(Building building, WeaponTier tier, int num_weapons, boolean infinite) {
        }

        @Override
        public void doMagic(Unit chieftain, MagicType magic, boolean clearStack) {
        }

        @Override
        public void exitTower(Building building) {
        }

        @Override
        public void trainChieftain(Building building, boolean start) {
        }

        @Override
        public void placeBuilding(List<? extends Selectable<?>> selection, BuildingType template_type,
                int placing_grid_x, int placing_grid_y) {
            placedBuildings.add(new RecordedPlaceBuilding(selection, template_type, placing_grid_x, placing_grid_y));
        }

        @Override
        public void setRallyPoint(Building building, Target target) {
        }

        @Override
        public void setTarget(List<? extends Selectable<?>> selection, Target target, Action action,
                boolean aggressive) {
            entityTargets.add(new RecordedEntityTarget(selection, target, action, aggressive));
        }

        @Override
        public void setTarget(Selectable<?> unit, Target target, Action action, boolean aggressive) {
        }

        @Override
        public void setRallyPoint(Building building, int grid_x, int grid_y) {
        }

        @Override
        public void setLandscapeTarget(List<? extends Selectable<?>> selection, int grid_x, int grid_y,
                Action action, boolean aggressive) {
            landscapeTargets.add(new RecordedLandscapeTarget(selection, grid_x, grid_y, action, aggressive));
        }

        @Override
        public void setLandscapeTarget(Selectable<?> unit, int grid_x, int grid_y, Action action,
                boolean aggressive) {
        }

        @Override
        public void setPreferredGamespeed(int speed) {
        }

        @Override
        public void changePreferredGamespeed(int delta) {
        }
    }
}
