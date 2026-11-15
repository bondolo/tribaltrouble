package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.DistributableTable;
import com.oddlabs.tt.simulation.model.EntityActivity;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.model.WeaponTier;
import com.oddlabs.util.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PlayerCommandExecutor} verifying command validation, entity resolution, and execution.
 */
class PlayerCommandExecutorTest {

    private World world;
    private DistributableTable table;
    private Player player0;
    private Player player1;
    private List<String> rejections;

    @BeforeEach
    void setUp() {
        int gridSize = 64;
        Landscape landscape = FlatLandscape.create(gridSize,
                FlatLandscape.generateStartingLocations(2, 20, 25.0f, 40.0f));
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

        table = world.getDistributableTable();
        player0 = world.getPlayers().get(0);
        player1 = world.getPlayers().get(1);

        UnitInfo unitInfo = new UnitInfo(true, true, 1, true, 3, 2, 1, 0);
        player0.initializeStartingBase(unitInfo);
        player1.initializeStartingBase(unitInfo);

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
        rejections.clear();

        // Unsupported deploy type for quarters (warriors require armory)
        player0.execute(new PlayerCommand.DeployUnits(quartersId, DeployType.ROCK_WARRIOR, 1));
        assertEquals(List.of("deployUnits:UNSUPPORTED_ACTION"), rejections);
        rejections.clear();

        // Unsupported building (tower)
        Building tower = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Building b && b.getTemplate().getBuildingType() == BuildingType.TOWER)
                .map(s -> (Building) s)
                .findFirst()
                .orElseThrow();
        int towerId = table.findName(tower).orElseThrow();
        player0.execute(new PlayerCommand.DeployUnits(towerId, DeployType.PEON, 1));
        assertEquals(List.of("deployUnits:UNSUPPORTED_ACTION"), rejections);
        rejections.clear();

        // Zero and Integer.MIN_VALUE amounts must be rejected
        player0.execute(new PlayerCommand.DeployUnits(quartersId, DeployType.PEON, 0));
        assertEquals(List.of("deployUnits:INVALID_AMOUNT"), rejections);
        rejections.clear();

        player0.execute(new PlayerCommand.DeployUnits(quartersId, DeployType.PEON, Integer.MIN_VALUE));
        assertEquals(List.of("deployUnits:INVALID_AMOUNT"), rejections);
        rejections.clear();

        // Large positive amount must clamp cleanly without overflow
        player0.execute(new PlayerCommand.DeployUnits(quartersId, DeployType.PEON, Integer.MAX_VALUE));
        assertTrue(rejections.isEmpty());
    }

    @Test
    void testPlaceBuildingAndCoordinatesValidation() {
        Unit peon = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Unit u && u.getTemplate().getUnitType() == UnitType.PEON)
                .map(s -> (Unit) s)
                .findFirst()
                .orElseThrow();
        int peonId = table.findName(peon).orElseThrow();

        // Invalid grid coordinate rejection notifies OUT_OF_BOUNDS
        player0.execute(new PlayerCommand.PlaceBuilding(new int[]{peonId}, BuildingType.TOWER, -1, 10));
        assertTrue(rejections.contains("placeBuilding:OUT_OF_BOUNDS"));
        rejections.clear();
        player0.execute(new PlayerCommand.PlaceBuilding(new int[]{peonId}, BuildingType.TOWER, 10, 1000));
        assertTrue(rejections.contains("placeBuilding:OUT_OF_BOUNDS"));
        rejections.clear();

        // Illegal placement (overlapping existing quarters) notifies ILLEGAL_PLACEMENT
        Building quarters = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Building b && b.getTemplate().getBuildingType() == BuildingType.QUARTERS)
                .map(s -> (Building) s)
                .findFirst()
                .orElseThrow();
        player0.execute(new PlayerCommand.PlaceBuilding(new int[]{peonId}, BuildingType.TOWER,
                quarters.getGridX(), quarters.getGridY()));
        assertEquals(List.of("placeBuilding:ILLEGAL_PLACEMENT"), rejections);
        rejections.clear();

        // Non-builder unit (chieftain) cannot place buildings
        Unit chieftain = player0.getChieftain().orElseThrow();
        int chieftainId = table.findName(chieftain).orElseThrow();
        player0.execute(new PlayerCommand.PlaceBuilding(new int[]{chieftainId}, BuildingType.TOWER,
                quarters.getGridX(), quarters.getGridY()));
        assertEquals(List.of("placeBuilding:UNSUPPORTED_ACTION"), rejections);
        rejections.clear();

        // Valid placement orders peon to target the new building construction site
        var towerTemplate = player0.getRaceInfo().getBuildingTemplate(BuildingType.TOWER);
        int validX = -1;
        int validY = -1;
        for (int dx = 4; dx <= 12 && validX == -1; dx++) {
            for (int dy = -6; dy <= 6 && validX == -1; dy++) {
                int cx = quarters.getGridX() + dx;
                int cy = quarters.getGridY() + dy;
                if (Building.isPlacingLegal(world.getUnitGrid(), towerTemplate, cx, cy)) {
                    validX = cx;
                    validY = cy;
                }
            }
        }
        assertNotEquals(-1, validX, "Expected a legal building placement near quarters");
        player0.execute(new PlayerCommand.PlaceBuilding(new int[]{peonId}, BuildingType.TOWER, validX, validY));
        assertTrue(peon.getActivity() == EntityActivity.PLACING_BUILDING
                && peon.getPlacingBuilding() != null
                && peon.getPlacingBuilding().getBuildingType() == BuildingType.TOWER);
        assertTrue(rejections.isEmpty());
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

        player0.execute(new PlayerCommand.DoMagic(chiefId, MagicType.STUN));
        assertTrue(rejections.isEmpty());

        // Reject magic not belonging to player's race
        player0.execute(new PlayerCommand.DoMagic(chiefId, MagicType.LIGHTNING_CLOUD));
        assertEquals(List.of("doMagic:UNSUPPORTED_ACTION"), rejections);
        rejections.clear();

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
    void testPlayerCommandExecutorDirectDispatch() {
        PlayerCommandExecutor executor = player0;

        Building quarters = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Building b && b.getTemplate().getBuildingType() == BuildingType.QUARTERS)
                .map(s -> (Building) s)
                .findFirst()
                .orElseThrow();
        int quartersId = table.findName(quarters).orElseThrow();

        // Execution via PlayerCommandExecutor interface reference
        executor.execute(new PlayerCommand.DeployUnits(quartersId, DeployType.PEON, 1));
        assertTrue(rejections.isEmpty());

        executor.execute(new PlayerCommand.DeployUnits(99999, DeployType.PEON, 1));
        assertEquals(List.of("deployUnits:ENTITY_NOT_FOUND"), rejections);
        rejections.clear();

        // Gamespeed execution via executor
        executor.execute(new PlayerCommand.SetPreferredGamespeed(4));
        assertEquals(4, player0.getPreferredGamespeed());
        executor.execute(new PlayerCommand.ChangePreferredGamespeed(-1));
        assertEquals(3, player0.getPreferredGamespeed());

        // Single-unit landscape command via executor
        Unit peon = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Unit u && u.getTemplate().getUnitType() == UnitType.PEON)
                .map(s -> (Unit) s)
                .findFirst()
                .orElseThrow();
        int peonId = table.findName(peon).orElseThrow();
        executor.execute(new PlayerCommand.SetLandscapeTarget(new int[]{peonId}, 14, 14, Action.MOVE, false));
        assertTrue(rejections.isEmpty());
    }

    @Test
    void testPlayerDirectDomainMethodsDispatchViaCommandPipeline() {
        Building quarters = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Building b && b.getTemplate().getBuildingType() == BuildingType.QUARTERS)
                .map(s -> (Building) s)
                .findFirst()
                .orElseThrow();

        // Calling PlayerInterface domain methods directly on player0 routes through the command pipeline
        player0.deployUnits(quarters, DeployType.PEON, 1);
        assertTrue(rejections.isEmpty());

        PlayerInterface pi = player0;
        pi.deployUnits(quarters, DeployType.PEON, 1);
        assertTrue(rejections.isEmpty());
    }

    @Test
    void testDirectExecutionRejectionParity() {
        Building enemyQuarters = player1.getUnits().getSet().stream()
                .filter(s -> s instanceof Building b && b.getTemplate().getBuildingType() == BuildingType.QUARTERS)
                .map(s -> (Building) s)
                .findFirst()
                .orElseThrow();
        Unit peon = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Unit u && u.getTemplate().getUnitType() == UnitType.PEON)
                .map(s -> (Unit) s)
                .findFirst()
                .orElseThrow();
        Unit enemyPeon = player1.getUnits().getSet().stream()
                .filter(s -> s instanceof Unit u && u.getTemplate().getUnitType() == UnitType.PEON)
                .map(s -> (Unit) s)
                .findFirst()
                .orElseThrow();

        // Direct domain method rejection on unowned building
        player0.deployUnits(enemyQuarters, DeployType.PEON, 1);
        assertEquals(List.of("deployUnits:NOT_OWNED"), rejections);
        rejections.clear();

        player0.createHarvesters(enemyQuarters, 1, 0, 0, 0);
        assertEquals(List.of("createHarvesters:NOT_OWNED"), rejections);
        rejections.clear();

        player0.buildWeapons(enemyQuarters, WeaponTier.ROCK, 1, false);
        assertEquals(List.of("buildWeapons:NOT_OWNED"), rejections);
        rejections.clear();

        Building quarters = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Building b && b.getTemplate().getBuildingType() == BuildingType.QUARTERS)
                .map(s -> (Building) s)
                .findFirst()
                .orElseThrow();
        int quartersId = table.findName(quarters).orElseThrow();
        player0.execute(new PlayerCommand.BuildWeapons(quartersId, WeaponTier.ROCK, 0, false));
        assertEquals(List.of("buildWeapons:INVALID_AMOUNT"), rejections);
        rejections.clear();

        player0.execute(new PlayerCommand.BuildWeapons(quartersId, WeaponTier.ROCK, Integer.MIN_VALUE, false));
        assertEquals(List.of("buildWeapons:INVALID_AMOUNT"), rejections);
        rejections.clear();

        player0.execute(new PlayerCommand.BuildWeapons(quartersId, WeaponTier.ROCK, 1, false));
        assertEquals(List.of("buildWeapons:UNSUPPORTED_ACTION"), rejections);
        rejections.clear();

        // Unsupported magic, chieftain training, and tower exit rejections
        int peonId = table.findName(peon).orElseThrow();
        player0.execute(new PlayerCommand.DoMagic(peonId, MagicType.STUN));
        assertEquals(List.of("doMagic:UNSUPPORTED_ACTION"), rejections);
        rejections.clear();

        player0.execute(new PlayerCommand.ExitTower(quartersId));
        assertEquals(List.of("exitTower:UNSUPPORTED_ACTION"), rejections);
        rejections.clear();

        player0.execute(new PlayerCommand.TrainChieftain(quartersId, false));
        assertEquals(List.of("trainChieftain:UNSUPPORTED_ACTION"), rejections);
        rejections.clear();

        // Tower cannot have a rally point
        Building tower = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Building b && b.getTemplate().getBuildingType() == BuildingType.TOWER)
                .map(s -> (Building) s)
                .findFirst()
                .orElseThrow();
        int towerId = table.findName(tower).orElseThrow();
        player0.execute(new PlayerCommand.SetRallyPointCoords(towerId, quarters.getGridX(), quarters.getGridY()));
        assertEquals(List.of("setRallyPoint:UNSUPPORTED_ACTION"), rejections);
        rejections.clear();

        // Duplicate selection IDs are deduplicated cleanly
        player0.execute(new PlayerCommand.SetLandscapeTarget(
                new int[]{peonId, peonId, peonId}, quarters.getGridX(), quarters.getGridY(), Action.MOVE, false));
        assertTrue(rejections.isEmpty());

        // Out of bounds placement rejection
        player0.placeBuilding(List.of(peon), BuildingType.TOWER, -1, 10);
        assertEquals(List.of("placeBuilding:OUT_OF_BOUNDS"), rejections);
        rejections.clear();

        // Unowned builder placement rejection
        player0.placeBuilding(List.of(enemyPeon), BuildingType.TOWER, 15, 15);
        assertEquals(List.of("placeBuilding:NOT_OWNED"), rejections);
        rejections.clear();

        // Out of bounds landscape target rejection
        player0.setLandscapeTarget(peon, -1, 10, Action.MOVE, false);
        assertEquals(List.of("setLandscapeTarget:OUT_OF_BOUNDS"), rejections);
        rejections.clear();

        // Unowned unit landscape target rejection
        player0.setLandscapeTarget(enemyPeon, 10, 10, Action.MOVE, false);
        assertEquals(List.of("setLandscapeTarget:NOT_OWNED"), rejections);
        rejections.clear();

        // Null target rejection
        player0.setTarget(peon, null, Action.MOVE, false);
        assertEquals(List.of("setTarget:ENTITY_NOT_FOUND"), rejections);
        rejections.clear();

        int enemyPeonId = table.findName(enemyPeon).orElseThrow();
        int enemyChiefId = table.findName(player1.getChieftain().orElseThrow()).orElseThrow();

        // Execution via PlayerCommand on unowned unit must report NOT_OWNED
        player0.execute(new PlayerCommand.PlaceBuilding(new int[]{enemyPeonId}, BuildingType.TOWER, 15, 15));
        assertEquals(List.of("placeBuilding:NOT_OWNED"), rejections);
        rejections.clear();

        player0.execute(new PlayerCommand.SetTarget(new int[]{enemyPeonId}, enemyChiefId, Action.ATTACK, true));
        assertEquals(List.of("setTarget:NOT_OWNED"), rejections);
        rejections.clear();

        player0.execute(new PlayerCommand.SetLandscapeTarget(new int[]{enemyPeonId}, 10, 10, Action.MOVE, false));
        assertEquals(List.of("setLandscapeTarget:NOT_OWNED"), rejections);
        rejections.clear();
    }

    @Test
    void testCreateHarvestersValidation() {
        Building armory = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Building b && b.getTemplate().getBuildingType() == BuildingType.ARMORY)
                .map(s -> (Building) s)
                .findFirst()
                .orElseThrow();
        Building tower = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Building b && b.getTemplate().getBuildingType() == BuildingType.TOWER)
                .map(s -> (Building) s)
                .findFirst()
                .orElseThrow();
        int armoryId = table.findName(armory).orElseThrow();
        int towerId = table.findName(tower).orElseThrow();

        // Negative counts must be rejected
        player0.execute(new PlayerCommand.CreateHarvesters(armoryId, -1, 0, 0, 0));
        assertEquals(List.of("createHarvesters:INVALID_AMOUNT"), rejections);
        rejections.clear();

        // Exceeding available peon supply in armory must be rejected
        int available = armory.getUnitContainer().orElseThrow().getNumSupplies();
        player0.execute(new PlayerCommand.CreateHarvesters(armoryId, available + 1, 0, 0, 0));
        assertEquals(List.of("createHarvesters:INSUFFICIENT_SUPPLY"), rejections);
        rejections.clear();

        // Integer overflow sum must also be rejected
        player0.execute(new PlayerCommand.CreateHarvesters(armoryId, Integer.MAX_VALUE, 1, 0, 0));
        assertEquals(List.of("createHarvesters:INSUFFICIENT_SUPPLY"), rejections);
        rejections.clear();

        // Non-worker building (tower) must be rejected
        player0.execute(new PlayerCommand.CreateHarvesters(towerId, 1, 0, 0, 0));
        assertEquals(List.of("createHarvesters:INSUFFICIENT_SUPPLY"), rejections);
        rejections.clear();

        // Valid conversion when peon supply is present
        armory.getUnitContainer().orElseThrow().increaseSupply(2);
        player0.execute(new PlayerCommand.CreateHarvesters(armoryId, 1, 1, 0, 0));
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

    @Test
    void testOutOfBoundsCoordinatesRejectCommand() {
        Building quarters = player0.getUnits().getSet().stream()
                .filter(s -> s instanceof Building b && b.getTemplate().getBuildingType() == BuildingType.QUARTERS)
                .map(s -> (Building) s)
                .findFirst()
                .orElseThrow();
        int quartersId = table.findName(quarters).orElseThrow();

        int n = world.getHeightMap().getGridUnitsPerWorld();
        assertEquals(n, world.getUnitGrid().getGridSize(), "HeightMap and UnitGrid sizes must be identical");

        // Coordinates strictly on the boundaries: (0, 0), (N-1, 0), (0, N-1), (N-1, N-1)
        int[][] validCorners = {{0, 0}, {n - 1, 0}, {0, n - 1}, {n - 1, n - 1}};
        for (int[] corner : validCorners) {
            int x = corner[0];
            int y = corner[1];

            rejections.clear();
            player0.execute(new PlayerCommand.SetRallyPointCoords(quartersId, x, y));
            assertTrue(rejections.stream().noneMatch(r -> r.contains("OUT_OF_BOUNDS")),
                    "Valid corner (" + x + ", " + y + ") must not be rejected with OUT_OF_BOUNDS for setRallyPoint");

            rejections.clear();
            player0.execute(new PlayerCommand.PlaceBuilding(new int[0], BuildingType.QUARTERS, x, y));
            assertTrue(rejections.stream().noneMatch(r -> r.contains("OUT_OF_BOUNDS")),
                    "Valid corner (" + x + ", " + y + ") must not be rejected with OUT_OF_BOUNDS for placeBuilding");

            rejections.clear();
            player0.execute(new PlayerCommand.SetLandscapeTarget(new int[0], x, y, Action.MOVE, false));
            assertTrue(rejections.stream().noneMatch(r -> r.contains("OUT_OF_BOUNDS")),
                    "Valid corner (" + x + ", " + y
                            + ") must not be rejected with OUT_OF_BOUNDS for setLandscapeTarget");
        }

        // Coordinates strictly outside the boundaries: negative and >= N
        int[][] invalidCoords = {{-1, 0}, {0, -1}, {-1, -1}, {n, 0}, {0, n}, {n, n}, {1000, 10}};
        for (int[] invalid : invalidCoords) {
            int x = invalid[0];
            int y = invalid[1];

            rejections.clear();
            player0.execute(new PlayerCommand.PlaceBuilding(new int[0], BuildingType.QUARTERS, x, y));
            assertTrue(rejections.contains("placeBuilding:OUT_OF_BOUNDS"),
                    "Invalid coordinate (" + x + ", " + y + ") must be rejected with OUT_OF_BOUNDS for placeBuilding");

            rejections.clear();
            player0.execute(new PlayerCommand.SetRallyPointCoords(quartersId, x, y));
            assertTrue(rejections.contains("setRallyPoint:OUT_OF_BOUNDS"),
                    "Invalid coordinate (" + x + ", " + y + ") must be rejected with OUT_OF_BOUNDS for setRallyPoint");

            rejections.clear();
            player0.execute(new PlayerCommand.SetLandscapeTarget(new int[0], x, y, Action.MOVE, false));
            assertTrue(rejections.contains("setLandscapeTarget:OUT_OF_BOUNDS"),
                    "Invalid coordinate (" + x + ", " + y
                            + ") must be rejected with OUT_OF_BOUNDS for setLandscapeTarget");
        }
    }

    @Test
    void testWorldTurnCommandBufferIntegration() {
        int tick = world.getTick();
        world.getTurnCommandBuffer().schedule(tick, 0, new PlayerCommand.DeployUnits(99999, DeployType.PEON, 1));
        assertEquals(1, world.getTurnCommandBuffer().getScheduledTickCount());
        assertTrue(rejections.isEmpty());

        world.tick();

        assertEquals(0, world.getTurnCommandBuffer().getScheduledTickCount());
        assertTrue(rejections.contains("deployUnits:ENTITY_NOT_FOUND"));
    }
}
