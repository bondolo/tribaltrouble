package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.state.CastingMagic;
import com.oddlabs.tt.simulation.state.Harvesting;
import com.oddlabs.tt.simulation.state.Hunting;
import com.oddlabs.tt.simulation.state.Idling;
import com.oddlabs.tt.simulation.state.Stunned;
import com.oddlabs.tt.simulation.state.Walking;
import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.WorldMerkleTree.BuildingNode;
import com.oddlabs.tt.simulation.model.WorldMerkleTree.UnitNode;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Characterization tests locking down legacy `Controller`/`Behaviour` state machine invariants.
@NullMarked
final class UnitStateMachineCharacterizationTest {

    @Test
    void testZeroTickPlaceBuildingRepairHarvestWalkCascade() {
        World worldA = createCleanWorld();
        World worldB = createCleanWorld();

        WorldMerkleTree snapshotA = runPlaceBuildingCascade(worldA);
        WorldMerkleTree snapshotB = runPlaceBuildingCascade(worldB);

        assertEquals(snapshotA.rootHash(), snapshotB.rootHash(),
                "Zero-tick cascade must produce bit-identical Merkle state");
        assertTrue(WorldMerkleTree.diff(snapshotA, snapshotB).isEmpty());
    }

    private static WorldMerkleTree runPlaceBuildingCascade(World world) {
        Player player = world.getPlayers().getFirst();
        world.spawnTree(UnitGrid.coordinateFromGrid(24), UnitGrid.coordinateFromGrid(16));

        int siteGridX = 16;
        int siteGridY = 16;
        // Peon placed just outside the 3x3 tower interior but within building.getSize() (~2.93)
        Unit peon = new Unit(player,
                UnitGrid.coordinateFromGrid(siteGridX + 2),
                UnitGrid.coordinateFromGrid(siteGridY),
                null,
                player.getRaceInfo().getUnitTemplate(UnitType.PEON));
        assertEquals(0, peon.getSupplyContainer().getNumSupplies());

        BuildingTemplate towerTemplate = player.getRaceInfo().getBuildingTemplate(BuildingType.TOWER);
        Building building = new Building(player, towerTemplate, siteGridX, siteGridY);
        assertFalse(building.isPlaced());

        int tickBefore = world.getTick();
        peon.initTarget(building, Action.DEFAULT, false);

        assertEquals(tickBefore, world.getTick());
        assertTrue(building.isPlaced(), "PlacingBuilding must place building in 0 ticks");
        assertEquals(Harvesting.class, peon.getCurrentState().getClass());
        assertTrue(peon.isMoving());
        assertFalse(peon.isShouldDecide());

        WorldMerkleTree snapshot = WorldMerkleTree.capture(world);
        UnitNode peonNode = snapshot.players().getFirst().units().stream()
                .filter(u -> u.entityId() == peon.getId())
                .findFirst()
                .orElseThrow();
        assertEquals("Harvesting", peonNode.primaryController());
        assertEquals("Harvesting", peonNode.currentController());
        assertEquals("WalkingPhase", peonNode.currentBehaviour());
        assertFalse(peonNode.shouldDecide());

        BuildingNode buildingNode = snapshot.players().getFirst().buildings().stream()
                .filter(b -> b.entityId() == building.getId())
                .findFirst()
                .orElseThrow();
        assertEquals(1, buildingNode.buildPoints());
        assertEquals(EntityActivity.UNDER_CONSTRUCTION, buildingNode.activity());
        return snapshot;
    }

    @Test
    void testUninterruptibleDeferralVsForceDecidePreemption() {
        World world = createCleanWorld();
        Player viking = world.getPlayers().getFirst();
        Player enemy = world.getPlayers().get(1);

        Unit walker = new Unit(viking,
                UnitGrid.coordinateFromGrid(16),
                UnitGrid.coordinateFromGrid(16),
                null,
                viking.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        walker.initTarget(new LandscapeTarget(26, 16), Action.MOVE, false);

        world.tick();
        assertEquals("UNINTERRUPTIBLE", walker.getLastBehaviourStateName());
        assertFalse(walker.isShouldDecide());
        var initialWalkState = walker.getCurrentState();
        assertInstanceOf(Walking.class, initialWalkState);
        assertTrue(walker.isMoving());

        walker.initTarget(new LandscapeTarget(16, 26), Action.MOVE, false);
        assertTrue(walker.isShouldDecide(), "Command during UNINTERRUPTIBLE must latch should_decide");
        assertSame(initialWalkState, walker.getCurrentState(),
                "State must not switch until the UNINTERRUPTIBLE step completes");

        for (int i = 0; i < 60 && walker.isShouldDecide(); i++) {
            world.tick();
        }
        assertFalse(walker.isShouldDecide(), "Deferred decide must clear once step reaches boundary");
        assertNotSame(initialWalkState, walker.getCurrentState());
        assertInstanceOf(Walking.class, walker.getCurrentState());
        assertTrue(walker.isMoving());

        // Advance until mid-step UNINTERRUPTIBLE again, then verify stun() forceStateTransition() preempts immediately
        for (int i = 0; i < 10 && !"UNINTERRUPTIBLE".equals(walker.getLastBehaviourStateName()); i++) {
            world.tick();
        }
        assertEquals("UNINTERRUPTIBLE", walker.getLastBehaviourStateName());

        walker.stun(1.0f);
        assertFalse(walker.isShouldDecide(), "forceStateTransition() must clear deferred decide flag");
        assertEquals(EntityActivity.STUNNED, walker.getActivity());
        assertEquals(Stunned.class, walker.getCurrentState().getClass());

        // Verify fatal hit() forceDecide() preempts UNINTERRUPTIBLE movement immediately
        Unit victim = new Unit(viking,
                UnitGrid.coordinateFromGrid(30),
                UnitGrid.coordinateFromGrid(30),
                null,
                viking.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        victim.initTarget(new LandscapeTarget(40, 30), Action.MOVE, false);
        world.tick();
        assertEquals("UNINTERRUPTIBLE", victim.getLastBehaviourStateName());

        victim.hit(1000, 1f, 0f, enemy);
        assertTrue(victim.isDead());
        assertFalse(victim.isShouldDecide());
        assertEquals(EntityActivity.DEAD, victim.getActivity());
    }

    @Test
    void testIdleAggroLeashAndActivityReporting() {
        World world = createCleanWorld();
        Player viking = world.getPlayers().getFirst();
        Player enemy = world.getPlayers().get(1);

        Unit warrior = new Unit(viking,
                UnitGrid.coordinateFromGrid(16),
                UnitGrid.coordinateFromGrid(16),
                null,
                viking.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        // Distance 8 is within UNIT_RANGE (8) for scanVicinity, but outside throw range (~7.9).
        // Stun the enemy peon so it remains stationary at (24, 16) while warrior initiates pursuit.
        Unit enemyPeon = new Unit(enemy,
                UnitGrid.coordinateFromGrid(24),
                UnitGrid.coordinateFromGrid(16),
                null,
                enemy.getRaceInfo().getUnitTemplate(UnitType.PEON));
        enemyPeon.stun(5.0f);

        assertEquals(EntityActivity.IDLE, warrior.getActivity());

        for (int i = 0; i < 150 && warrior.getCurrentState() instanceof Idling; i++) {
            world.tick();
        }

        assertEquals(Hunting.class, warrior.getCurrentState().getClass());
        assertTrue(warrior.isAggressiveWalking());
        assertTrue(warrior.isAttacking());
        assertEquals(EntityActivity.ACTIVE, warrior.getActivity());

        // Advance until warrior walks closer to enemy and then enemy dies; Hunting transitions to Walking leash
        for (int i = 0; i < 50 && warrior.getGridX() == 16; i++) {
            world.tick();
        }
        assertTrue(warrior.getGridX() > 16, "Warrior should have stepped away from idle leash post");
        enemyPeon.hit(1000, 1f, 0f, viking);
        for (int i = 0; i < 50 && warrior.getCurrentState() instanceof Hunting; i++) {
            world.tick();
        }

        assertEquals(Walking.class, warrior.getCurrentState().getClass());
        assertTrue(warrior.isAggressiveWalking());
        assertEquals(EntityActivity.ATTACKING_MOVING, warrior.getActivity());
    }

    @Test
    void testDoubleStunStacksDurationAdditively() {
        World world = createCleanWorld();
        Player viking = world.getPlayers().getFirst();
        assertEquals(0.02f, world.getSecondsPerTick(), 1e-6f);

        Unit unit = new Unit(viking,
                UnitGrid.coordinateFromGrid(16),
                UnitGrid.coordinateFromGrid(16),
                null,
                viking.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));

        unit.stun(1.0f);
        assertEquals(EntityActivity.STUNNED, unit.getActivity());

        // Advance 0.50s (25 ticks) -> ~0.50s remaining on first stun
        tickN(world, 25);
        assertEquals(EntityActivity.STUNNED, unit.getActivity());

        // Apply second 1.0f stun on top of the paused first stun
        unit.stun(1.0f);
        assertEquals(EntityActivity.STUNNED, unit.getActivity());

        // Advance 1.10s (55 ticks, 1.60s total elapsed): in legacy mode first stun resumes (additive 1.0s + 1.0s);
        // in modern mode the second 1.0s stun refreshed to max(1.0s, 0.5s) = 1.0s and has now expired.
        tickN(world, 55);
        if (AppConfig.LEGACY_DETERMINISTIC) {
            assertEquals(EntityActivity.STUNNED, unit.getActivity(),
                    "First stun must resume after second stun expires (additive 1.0s + 1.0s duration)");
            assertTrue(unit.isStunned());

            // Advance remaining 0.50s (25 ticks, 2.10s total elapsed): first stun expires
            tickN(world, 25);
        }
        assertEquals(EntityActivity.IDLE, unit.getActivity());
        assertFalse(unit.isStunned());
    }

    @Test
    void testReEntrantEnterBuildingAndMagicForceInterruptedCleanup() {
        World world = createCleanWorld();
        Player viking = world.getPlayers().getFirst();
        Player enemy = world.getPlayers().get(1);

        // 1. Warrior entering friendly Tower -> mount() transitions cleanly to tower IdleController
        Building tower = new Building(viking,
                viking.getRaceInfo().getBuildingTemplate(BuildingType.TOWER),
                16,
                16);
        tower.place();
        tower.repair(tower.getTemplate().getMaxHitPoints());
        assertTrue(tower.isBuilt());

        Unit warrior = new Unit(viking,
                UnitGrid.coordinateFromGrid(18),
                UnitGrid.coordinateFromGrid(16),
                null,
                viking.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
        warrior.initTarget(tower, Action.DEFAULT, false);

        assertTrue(warrior.isMounted());
        assertFalse(warrior.isDead());
        assertEquals(1, tower.getUnitContainer().orElseThrow().getNumSupplies());
        assertEquals(Idling.class, warrior.getCurrentState().getClass());
        assertEquals(Idling.class, warrior.getStateMachine().getActiveOrPendingState().getClass());
        world.tick();
        assertTrue(warrior.isMounted());

        // 2. Peon entering friendly Quarters -> removeNow() & ReproduceUnitContainer count increments
        Building quarters = new Building(viking,
                viking.getRaceInfo().getBuildingTemplate(BuildingType.QUARTERS),
                32,
                32);
        quarters.place();
        quarters.repair(quarters.getTemplate().getMaxHitPoints());
        assertTrue(quarters.isBuilt());
        int initialQuartersSupplies = quarters.getUnitContainer().orElseThrow().getNumSupplies();

        Unit peon = new Unit(viking,
                UnitGrid.coordinateFromGrid(36),
                UnitGrid.coordinateFromGrid(32),
                null,
                viking.getRaceInfo().getUnitTemplate(UnitType.PEON));
        peon.initTarget(quarters, Action.DEFAULT, false);

        assertTrue(peon.isDead());
        assertEquals(initialQuartersSupplies + 1, quarters.getUnitContainer().orElseThrow().getNumSupplies());
        world.tick();

        // 3. Chieftain casting magic -> stun() / fatal hit() calls forceInterrupted() cleanly
        Unit chieftainA = new Unit(viking,
                UnitGrid.coordinateFromGrid(46),
                UnitGrid.coordinateFromGrid(46),
                null,
                viking.getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN));
        chieftainA.maxMagicEnergy(MagicType.STUN);
        chieftainA.doMagic(MagicType.STUN, false);
        assertEquals(CastingMagic.class, chieftainA.getCurrentState().getClass());

        world.tick();
        assertTrue(chieftainA.isCastingMagic());
        assertEquals("UNINTERRUPTIBLE", chieftainA.getLastBehaviourStateName());

        chieftainA.stun(1.0f);
        assertFalse(chieftainA.isShouldDecide());
        assertEquals(EntityActivity.STUNNED, chieftainA.getActivity());
        assertEquals(Stunned.class, chieftainA.getCurrentState().getClass());

        // Interrupting during CASTING phase (magic != null) cancels the active Magic instance
        Unit chieftainB = new Unit(viking,
                UnitGrid.coordinateFromGrid(52),
                UnitGrid.coordinateFromGrid(52),
                null,
                viking.getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN));
        Unit enemyObserver = new Unit(enemy,
                UnitGrid.coordinateFromGrid(53),
                UnitGrid.coordinateFromGrid(52),
                null,
                enemy.getRaceInfo().getUnitTemplate(UnitType.PEON));
        chieftainB.maxMagicEnergy(MagicType.STUN);
        chieftainB.doMagic(MagicType.STUN, true);

        // Advance into CASTING (past PREPARING ~2.15s, before release ~3.77s)
        tickN(world, 125);
        assertFalse(enemyObserver.isStunned());
        assertTrue(chieftainB.isCastingMagic());

        chieftainB.hit(1000, 1f, 0f, enemy);
        assertTrue(chieftainB.isDead());
        assertEquals(EntityActivity.DEAD, chieftainB.getActivity());

        tickN(world, 100);
        assertFalse(enemyObserver.isStunned(),
                "Interrupted MagicBehaviour must cancel spell before release");
    }

    private static void tickN(World world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.tick();
        }
    }

    private static World createCleanWorld() {
        int gridSize = 64;
        Landscape landscape = FlatLandscape.create(gridSize);
        WorldParameters worldParams = new WorldParameters("1", 5, 20);
        List<PlayerInfo> playerInfos = List.of(
                new PlayerInfo(0, Race.VIKINGS, "VikingPlayer"),
                new PlayerInfo(1, Race.NATIVES, "NativePlayer")
        );
        Color.Linear[] playerColors = new Color.Linear[]{
                new Color.Linear(1f, 0f, 0f, 1f),
                new Color.Linear(0f, 0f, 1f, 1f)
        };
        return new World(worldParams, landscape, PlayerConfig.from(playerInfos, playerColors));
    }
}
