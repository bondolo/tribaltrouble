package com.oddlabs.tt.simulation.state;

import static com.oddlabs.tt.simulation.model.Ability.ATTACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.AttackScanFilter;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.EntityActivity;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.tt.simulation.model.Supply;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.model.weapon.Magic;
import com.oddlabs.tt.simulation.model.weapon.MagicFactory;
import com.oddlabs.tt.simulation.model.weapon.Stun;
import com.oddlabs.tt.simulation.pathfinder.Movable;
import com.oddlabs.tt.simulation.pathfinder.PathTracker;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// Unit tests for `UnitState` implementations and `WalkingPhase`.
@NullMarked
final class UnitStatesTest {
  private World world;
  private Player vikingPlayer;
  private Player nativePlayer;
  private FakeUnitContext context;

  @BeforeEach
  void setUp() {
    createCleanWorld();
    context = new FakeUnitContext();
  }

  private void createCleanWorld() {
    Landscape landscape = FlatLandscape.create(64);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);
    List<PlayerInfo> playerInfos =
        List.of(new PlayerInfo(0, Race.VIKINGS, "VikingPlayer"), new PlayerInfo(1, Race.NATIVES, "NativePlayer"));
    Color.Linear[] playerColors =
        new Color.Linear[] {new Color.Linear(1f, 0f, 0f, 1f), new Color.Linear(0f, 0f, 1f, 1f)};
    world = new World(worldParams, landscape, PlayerConfig.from(playerInfos, playerColors));
    vikingPlayer = world.getPlayers().getFirst();
    nativePlayer = world.getPlayers().get(1);
  }

  private Unit spawnUnit(Player owner, int gridX, int gridY, UnitType unitType) {
    return new Unit(
        owner,
        UnitGrid.coordinateFromGrid(gridX),
        UnitGrid.coordinateFromGrid(gridY),
        null,
        owner.getRaceInfo().getUnitTemplate(unitType));
  }

  private Building placeBuiltBuilding(Player owner, BuildingType type, int gridX, int gridY) {
    Building building = new Building(owner, owner.getRaceInfo().getBuildingTemplate(type), gridX, gridY);
    building.place();
    building.repair(building.getTemplate().getMaxHitPoints());
    return building;
  }

  private TreeSupply spawnTree(int gridX, int gridY) {
    return world.spawnTree(UnitGrid.coordinateFromGrid(gridX), UnitGrid.coordinateFromGrid(gridY));
  }

  @Test
  void testDyingStateLifecycleAndSink() {
    Dying dying = new Dying();
    UnitStateMachine machine = new UnitStateMachine(dying, context);

    assertEquals(EntityActivity.DEAD, machine.getActivity());
    assertThrows(IllegalStateException.class, machine::isBlocking);
    assertEquals(Unit.Animation.DYING, context.getLastAnimation());
    assertEquals(1f / 3f, context.getLastAnimationSpeed(), 1e-6f);

    // DYING phase: 3.0s
    for (int i = 0; i < 150; i++) {
      machine.animate(0.02f, context);
      assertEquals(UnitState.StepStatus.UNINTERRUPTIBLE, machine.getLastStepStatus());
    }
    assertEquals(0f, machine.getOffsetZ(), 1e-5f);
    assertFalse(context.isDead());

    // Cross DYING -> LYING threshold (animTime < 0)
    machine.animate(0.02f, context);
    assertEquals(0f, machine.getOffsetZ(), 1e-5f);

    // LYING phase: 1.0s (50 ticks of 0.02s -> animTime transitions from ~0.98 down past 0)
    for (int i = 0; i < 50; i++) {
      machine.animate(0.02f, context);
    }
    assertFalse(context.isDead());
    assertFalse(context.getEvents().contains("removeFromWorld"));

    // MOVING phase: 60.0s sinking 3 meters (0.05 m/s)
    for (int i = 0; i < 3000; i++) {
      machine.animate(0.02f, context);
    }
    assertEquals(-3f, machine.getOffsetZ(), 0.01f);
    assertEquals(-3f, context.getOffsetZ(), 0.01f);

    // Final tick crosses MOVING -> removeFromWorld()
    machine.animate(0.02f, context);
    assertTrue(context.isDead());
    assertTrue(context.getEvents().contains("removeFromWorld"));
  }

  @Test
  void testStunnedStateCountdownAndPreemptZeroDuration() {
    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    UnitStateMachine machine = new UnitStateMachine(idle, context);

    // Zero-duration stun pops back immediately in zero ticks
    machine.forceTransition(Stunned.preempt(0f, machine.getCurrentState(), false), context);
    assertSame(idle, machine.getCurrentState());
    assertFalse(machine.isStunned());

    // 0.10s stun ticks down to fallback
    machine.forceTransition(Stunned.preempt(0.10f, machine.getCurrentState(), false), context);
    assertTrue(machine.isStunned());
    assertTrue(machine.isBlocking());
    assertEquals(EntityActivity.STUNNED, machine.getActivity());
    assertFalse(machine.isIdle());
    assertEquals(0.10f, machine.getStunTimeLeft(), 1e-5f);

    for (int i = 0; i < 4; i++) {
      machine.animate(0.02f, context);
      assertTrue(machine.isStunned());
      assertEquals(UnitState.StepStatus.UNINTERRUPTIBLE, machine.getLastStepStatus());
      assertEquals(Unit.Animation.IDLING, context.getLastAnimation());
    }
    machine.animate(0.02f, context);
    assertFalse(machine.isStunned());
    assertEquals(0f, machine.getStunTimeLeft(), 1e-5f);
    assertSame(idle, machine.getCurrentState());
  }

  @Test
  void testStunnedStateLegacyAdditiveVsModernMaxDuration() {
    Target post = new LandscapeTarget(5, 5);
    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    Defending defending = new Defending(post, idle);

    // Primary-order query delegation while stunned over Defending
    UnitStateMachine defendingMachine = new UnitStateMachine(defending, context);
    defendingMachine.forceTransition(Stunned.preempt(1.0f, defendingMachine.getCurrentState(), false), context);
    assertTrue(defendingMachine.isStunned());
    assertTrue(defendingMachine.isDefending());
    assertFalse(defendingMachine.isIdle());
    assertEquals(EntityActivity.STUNNED, defendingMachine.getActivity());

    // Legacy deterministic mode: additive double-stun stacking
    context.setLegacyDeterministic(true);
    UnitStateMachine legacyMachine = new UnitStateMachine(idle, context);
    legacyMachine.forceTransition(Stunned.preempt(1.0f, legacyMachine.getCurrentState(), true), context);
    legacyMachine.animate(0.4f, context);
    assertEquals(0.6f, legacyMachine.getStunTimeLeft(), 1e-5f);

    // Second 0.5s stun stacks on top of remaining 0.6s -> total remaining 1.1s
    legacyMachine.forceTransition(Stunned.preempt(0.5f, legacyMachine.getCurrentState(), true), context);
    assertEquals(0.5f, legacyMachine.getStunTimeLeft(), 1e-5f);
    legacyMachine.animate(0.5f, context);
    // Outer stun finished, inner Stunned resumes with 0.6s
    assertTrue(legacyMachine.isStunned());
    assertEquals(0.6f, legacyMachine.getStunTimeLeft(), 1e-5f);
    legacyMachine.animate(0.6f, context);
    assertFalse(legacyMachine.isStunned());
    assertSame(idle, legacyMachine.getCurrentState());

    // Modern mode: max-duration refresh without stacking
    context.setLegacyDeterministic(false);
    UnitStateMachine modernMachine = new UnitStateMachine(idle, context);
    modernMachine.forceTransition(Stunned.preempt(1.0f, modernMachine.getCurrentState(), false), context);
    modernMachine.animate(0.4f, context);
    assertEquals(0.6f, modernMachine.getStunTimeLeft(), 1e-5f);

    // Shorter 0.3s stun keeps existing 0.6s
    modernMachine.forceTransition(Stunned.preempt(0.3f, modernMachine.getCurrentState(), false), context);
    assertEquals(0.6f, modernMachine.getStunTimeLeft(), 1e-5f);
    // Longer 1.2s stun refreshes to 1.2s
    modernMachine.forceTransition(Stunned.preempt(1.2f, modernMachine.getCurrentState(), false), context);
    assertEquals(1.2f, modernMachine.getStunTimeLeft(), 1e-5f);
    modernMachine.animate(1.2f, context);
    assertFalse(modernMachine.isStunned());
    assertSame(idle, modernMachine.getCurrentState());
  }

  @Test
  void testCastingMagicStatePhasesAndForceInterrupt() {
    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    FakeMagicFactory factory = new FakeMagicFactory(1.0f, 2.0f, 3.0f);
    CastingMagic casting = new CastingMagic(factory, idle);

    UnitStateMachine machine = new UnitStateMachine(casting, context);
    assertTrue(machine.isCastingMagic());
    assertTrue(machine.isBlocking());
    assertEquals(EntityActivity.ACTIVE, machine.getActivity());
    assertEquals(Unit.Animation.MAGIC, context.getLastAnimation());
    assertEquals(1f / 3.0f, context.getLastAnimationSpeed(), 1e-6f);

    // Defer player order while uninterruptible
    Walking deferredWalk = new Walking(new LandscapeTarget(10, 10), false, idle);

    // PREPARING: t = 0 -> 1.0s
    machine.animate(0.5f, context);
    assertFalse(context.getEvents().contains("executeMagic"));
    machine.requestOrder(deferredWalk, context);
    assertSame(casting, machine.getCurrentState());
    assertTrue(machine.isShouldDecide());

    machine.animate(0.5f, context);
    assertTrue(context.getEvents().contains("executeMagic"));
    assertFalse(context.getEvents().contains("registerMagic"));

    // CASTING: t = 1.0 -> 2.0s (magicResult is null here; verifies null-safe path)
    machine.animate(0.5f, context);
    assertFalse(context.getEvents().contains("registerMagic"));
    machine.animate(0.5f, context);

    // Verify non-null Magic registration path in a second cast
    Unit chieftain =
        new Unit(vikingPlayer, 10f, 10f, null, vikingPlayer.getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN));
    AtomicBoolean removedOnInterrupt = new AtomicBoolean(false);
    world.setNotificationListener(
        new NotificationListener() {
          @Override
          public void onModelRemoved(Model model) {
            removedOnInterrupt.set(true);
          }
        });
    Stun stunMagic = new Stun(0f, 0f, 0f, 5f, 2f, 1f, chieftain);
    try {
      FakeUnitContext castContext = new FakeUnitContext();
      castContext.setMagicResult(stunMagic);
      CastingMagic secondCast = new CastingMagic(factory, idle);
      UnitStateMachine castMachine = new UnitStateMachine(secondCast, castContext);

      // Advance past PREPARING (1.0s) and CASTING (2.0s)
      castMachine.animate(1.0f, castContext);
      assertTrue(castContext.getEvents().contains("executeMagic"));
      assertFalse(castContext.getEvents().contains("registerMagic"));

      castMachine.animate(1.0f, castContext);
      assertTrue(castContext.getEvents().contains("registerMagic"));
      assertSame(stunMagic, castContext.getLastRegisteredMagic());
      assertTrue(castMachine.isCastingMagic());

      // Advance past ENDING (3.0s) -> pops to idle
      castMachine.animate(1.0f, castContext);
      assertSame(idle, castMachine.getCurrentState());

      // Now test forceTransition interrupting mid-CASTING calls magic.interrupt()
      Stun interruptedStun = new Stun(0f, 0f, 0f, 5f, 2f, 1f, chieftain);
      removedOnInterrupt.set(false);
      FakeUnitContext interruptContext = new FakeUnitContext();
      interruptContext.setMagicResult(interruptedStun);
      UnitStateMachine interruptMachine = new UnitStateMachine(new CastingMagic(factory, idle), interruptContext);
      interruptMachine.animate(1.0f, interruptContext); // enters CASTING with magic != null
      assertFalse(removedOnInterrupt.get());
      interruptMachine.forceTransition(new Dying(), interruptContext);
      assertTrue(removedOnInterrupt.get(), "onForceInterrupted must call magic.interrupt()");
    } finally {
      world.setNotificationListener(NotificationListener.EMPTY);
      chieftain.remove();
    }
  }

  @Test
  void testIdleStatePrngConsumptionAndAggroTransitions() {
    // 1. Unit without ATTACK still consumes 1 PRNG float per scan window
    // 1a. Modern mode: unit without ATTACK does not consume PRNG floats while idling
    context.setLegacyDeterministic(false);
    context.setRandomSeed(12345L);
    Random modernMirror = new Random(12345L);

    Idling modernPeonIdle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    UnitStateMachine modernPeonMachine = new UnitStateMachine(modernPeonIdle, context);
    assertTrue(modernPeonMachine.isIdle());
    assertFalse(context.getEvents().contains("scanForEnemy:" + AttackScanFilter.UNIT_RANGE));
    modernPeonMachine.animate(2.05f, context);
    assertEquals(modernMirror.nextFloat(), context.nextRandomFloat(), 1e-6f);

    // 1b. Legacy deterministic mode: unit without ATTACK still consumes 1 float per scan window
    FakeUnitContext legacyContext = new FakeUnitContext();
    legacyContext.setLegacyDeterministic(true);
    legacyContext.setRandomSeed(12345L);
    Random mirror = new Random(12345L);
    float expectedDelay = 1f + mirror.nextFloat();

    Idling peonIdle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    UnitStateMachine peonMachine = new UnitStateMachine(peonIdle, legacyContext);
    assertTrue(peonMachine.isIdle());
    assertFalse(legacyContext.getEvents().contains("scanForEnemy:" + AttackScanFilter.UNIT_RANGE));

    // Sleep until just before expectedDelay expires
    peonMachine.animate(expectedDelay - 0.05f, legacyContext);
    assertEquals(UnitState.StepStatus.INTERRUPTIBLE, peonMachine.getLastStepStatus());
    peonMachine.animate(0.06f, legacyContext);
    // Deciding again consumed the second float from PRNG; the next float must match mirror's third float
    mirror.nextFloat();
    assertEquals(mirror.nextFloat(), legacyContext.nextRandomFloat(), 1e-6f);

    // 2. Mobile unit with ATTACK spots enemy -> Hunting with Walking(leash, true) fallback
    Unit enemy = spawnUnit(nativePlayer, 20, 20, UnitType.WARRIOR_ROCK);
    try {
      FakeUnitContext warriorContext = new FakeUnitContext();
      warriorContext.addAbility(ATTACK);
      warriorContext.setGridPosition(10, 10);
      warriorContext.setScannedEnemy(enemy);

      Idling warriorIdle = new Idling(AttackScanFilter.UNIT_RANGE, true);
      UnitStateMachine warriorMachine = new UnitStateMachine(warriorIdle, warriorContext);

      Hunting hunting = assertInstanceOf(Hunting.class, warriorMachine.getCurrentState());
      assertTrue(warriorMachine.isAttacking());
      assertTrue(warriorMachine.isAggressiveWalking());

      // Enemy dies -> Hunting falls back to Walking(10, 10, scanAttack=true)
      enemy.hit(10000, 1f, 0f, vikingPlayer);
      warriorMachine.requestDecide(warriorContext);
      Walking leashWalk = assertInstanceOf(Walking.class, warriorMachine.getCurrentState());
      assertTrue(leashWalk.isScanAttack());
      assertEquals(10, leashWalk.getTarget().getGridX());
      assertEquals(10, leashWalk.getTarget().getGridY());
      assertEquals(EntityActivity.ATTACKING_MOVING, warriorMachine.getActivity());

      // Walk finishes -> pops back to Idling
      warriorContext.setScannedEnemy(null);
      warriorContext.setDefaultWalkState(PathTracker.State.DONE);
      warriorMachine.animate(0.02f, warriorContext);
      assertSame(warriorIdle, warriorMachine.getCurrentState());
    } finally {
      if (!enemy.isDead()) {
        enemy.remove();
      }
    }

    // 3. Tower-mounted unit (canMove == false) spots enemy -> stationary Hunting directly
    Unit towerEnemy = spawnUnit(nativePlayer, 11, 10, UnitType.WARRIOR_ROCK);
    try {
      FakeUnitContext towerContext = new FakeUnitContext();
      towerContext.addAbility(ATTACK);
      towerContext.setGridPosition(10, 10);
      towerContext.setAttackRange(3f);
      towerContext.setScannedEnemy(towerEnemy);

      Idling towerIdle = new Idling(AttackScanFilter.TOWER_RANGE, false);
      UnitStateMachine towerMachine = new UnitStateMachine(towerIdle, towerContext);
      assertInstanceOf(Hunting.class, towerMachine.getCurrentState());
      assertTrue(towerMachine.isAttacking());
    } finally {
      if (!towerEnemy.isDead()) {
        towerEnemy.remove();
      }
    }
  }

  @Test
  void testWalkingPhaseBackoffBlockerWakeAndOrders() {
    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    Target destination = new LandscapeTarget(20, 20);
    Walking walkOrder = new Walking(destination, false, idle);

    // Normal progression: OK (UNINTERRUPTIBLE) -> OK_INTERRUPTIBLE -> DONE -> fallback
    context.enqueueWalkSteps(PathTracker.State.OK, PathTracker.State.OK_INTERRUPTIBLE, PathTracker.State.DONE);
    UnitStateMachine machine = new UnitStateMachine(walkOrder, context);
    assertEquals(EntityActivity.MOVING, machine.getActivity());
    assertTrue(machine.isMoving());
    assertFalse(machine.isBlocking());

    machine.animate(0.02f, context);
    assertEquals(UnitState.StepStatus.UNINTERRUPTIBLE, machine.getLastStepStatus());
    assertEquals(Unit.Animation.MOVING, context.getLastAnimation());

    machine.animate(0.02f, context);
    assertEquals(UnitState.StepStatus.INTERRUPTIBLE, machine.getLastStepStatus());

    machine.animate(0.02f, context);
    assertSame(idle, machine.getCurrentState());

    // Exponential blocker retry backoff (0.5 -> 1.0 -> 2.0 -> 4.0 -> 5.0) and early wake on move
    FakeMovable blocker = new FakeMovable(12, 12);
    context.clearEvents();
    context.setWalkBlocker(blocker);
    context.setDefaultWalkState(PathTracker.State.BLOCKED);
    Walking blockedWalk = new Walking(destination, false, idle);
    UnitStateMachine blockedMachine = new UnitStateMachine(blockedWalk, context);

    // Tick 1: hits BLOCKED -> marks blocker, retryDelay = 1.0s, retryDelayCounter = 1.0s
    blockedMachine.animate(0.02f, context);
    assertTrue(blocker.isMarkedBlocking());
    assertTrue(blockedMachine.isBlocking());
    assertEquals(Unit.Animation.IDLING, context.getLastAnimation());

    // While retryDelayCounter > 0 and blocker hasn't moved, stepWalk is NOT called
    context.clearEvents();
    blockedMachine.animate(0.5f, context);
    assertFalse(context.getEvents().contains("stepWalk:BLOCKED"));

    // Wait remaining 0.5s -> hits BLOCKED again, retryDelay doubles to 2.0s
    blockedMachine.animate(0.5f, context);
    assertTrue(context.getEvents().contains("stepWalk:BLOCKED"));

    // Move blocker -> next tick wakes immediately even though 2.0s hasn't elapsed!
    blocker.setGridPosition(13, 12);
    context.clearEvents();
    context.enqueueWalkStep(PathTracker.State.OK_INTERRUPTIBLE);
    blockedMachine.animate(0.02f, context);
    assertTrue(context.getEvents().contains("stepWalk:OK_INTERRUPTIBLE"));
    assertFalse(blockedMachine.isBlocking());

    // Check max cap 5.0s directly on WalkingPhase
    context.initWalkTarget(destination, 0f);
    WalkingPhase phase = new WalkingPhase();
    phase.start(false);
    context.setWalkBlocker(null);
    context.setDefaultWalkState(PathTracker.State.BLOCKED);
    phase.step(0.02f, context, idle);
    assertEquals(1.0f, phase.getRetryDelay(), 1e-6f);
    phase.step(1.0f, context, idle);
    assertEquals(2.0f, phase.getRetryDelay(), 1e-6f);
    phase.step(2.0f, context, idle);
    assertEquals(4.0f, phase.getRetryDelay(), 1e-6f);
    phase.step(4.0f, context, idle);
    assertEquals(5.0f, phase.getRetryDelay(), 1e-6f);
    phase.step(5.0f, context, idle);
    assertEquals(5.0f, phase.getRetryDelay(), 1e-6f);
  }

  @Test
  void testDefendingStateAndScanAttackGiveUpReset() {
    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    Target guardPost = new LandscapeTarget(15, 15);
    Defending defending = new Defending(guardPost, idle);

    Unit enemy = spawnUnit(nativePlayer, 18, 18, UnitType.WARRIOR_ROCK);
    try {
      context.setGridPosition(15, 15);
      context.setAttackRange(2f);
      context.setDefaultWalkState(PathTracker.State.OK_INTERRUPTIBLE);

      UnitStateMachine machine = new UnitStateMachine(defending, context);
      assertTrue(machine.isDefending());
      assertEquals(EntityActivity.IDLE, machine.getActivity());

      // Spot enemy during OK_INTERRUPTIBLE -> transitions to Hunting, resets Defending giveUpTracker
      context.setScannedEnemy(enemy);
      machine.animate(0.02f, context);
      assertInstanceOf(Hunting.class, machine.getCurrentState());
      assertTrue(machine.isDefending());

      // Enemy dies -> pops back to Defending, which can walk again because giveUpTracker was reset
      enemy.hit(10000, 1f, 0f, vikingPlayer);
      context.setScannedEnemy(null);
      machine.requestDecide(context);
      assertSame(defending, machine.getCurrentState());

      // Now walk completes -> Defending pops to Idling on second decide
      context.setDefaultWalkState(PathTracker.State.DONE);
      machine.animate(0.02f, context);
      assertSame(idle, machine.getCurrentState());
    } finally {
      if (!enemy.isDead()) {
        enemy.remove();
      }
    }
  }

  @Test
  void testHuntingStateStationaryAndPursuitCombatCycle() {
    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, false);
    Unit enemy = spawnUnit(nativePlayer, 12, 10, UnitType.WARRIOR_ROCK);
    try {
      context.setGridPosition(10, 10);
      context.setAttackRange(3f);
      context.setWeaponSecondsPerRelease(0.88f);

      // 1. Stationary Hunting (canMove == false) throw release timing
      Hunting attacking = new Hunting(enemy, false, idle);
      UnitStateMachine attackMachine = new UnitStateMachine(attacking, context);
      assertTrue(attackMachine.isAttacking());
      assertFalse(attackMachine.isAttackWeaponReleased());
      assertSame(enemy, attackMachine.getAttackTarget());
      assertEquals(Unit.Animation.THROWING, context.getLastAnimation());

      // Before 0.88s: not released yet, uninterruptible
      attackMachine.animate(0.50f, context);
      assertFalse(attackMachine.isAttackWeaponReleased());
      assertFalse(context.getEvents().contains("launchWeaponAttack"));
      assertEquals(UnitState.StepStatus.UNINTERRUPTIBLE, attackMachine.getLastStepStatus());

      // Cross 0.88s release point
      attackMachine.animate(0.40f, context);
      assertTrue(attackMachine.isAttackWeaponReleased());
      assertTrue(context.getEvents().contains("launchWeaponAttack"));
      assertEquals(UnitState.StepStatus.UNINTERRUPTIBLE, attackMachine.getLastStepStatus());

      // Finish remainder of 2.0s attack cycle -> repeats new throw cycle because enemy is alive & in range
      context.clearEvents();
      attackMachine.animate(1.10f, context);
      assertSame(attacking, attackMachine.getCurrentState());
      assertFalse(attackMachine.isAttackWeaponReleased());
      assertTrue(context.getEvents().contains("switchAnimation:THROWING"));

      // Move out of range -> complete THROWING then RELEASED to pop to fallback
      context.setGridPosition(0, 0);
      attackMachine.animate(0.90f, context);
      attackMachine.animate(1.15f, context);
      assertSame(idle, attackMachine.getCurrentState());

      // 2. Hunting pursuit <-> attack switching and give-up reset
      Idling mobileIdle = new Idling(AttackScanFilter.UNIT_RANGE, true);
      context.setGridPosition(0, 0);
      context.setDefaultWalkState(PathTracker.State.OK_INTERRUPTIBLE);
      Hunting hunting = new Hunting(enemy, mobileIdle);
      UnitStateMachine huntMachine = new UnitStateMachine(hunting, context);

      // Out of range -> WalkingPhase pursuit
      assertTrue(huntMachine.isMoving());
      assertFalse(huntMachine.isAttackWeaponReleased());
      assertNull(huntMachine.getAttackTarget());

      // Move into attack range when walk step finishes
      context.setGridPosition(11, 10);
      context.setDefaultWalkState(PathTracker.State.DONE);
      huntMachine.animate(0.02f, context);

      // Now in AttackThrowPhase, give-up counter was reset
      assertFalse(huntMachine.isMoving());
      assertSame(enemy, huntMachine.getAttackTarget());

      // Enemy steps out of range after throw completes -> Hunting pursues again (because give-up reset)
      context.setGridPosition(0, 0);
      context.setDefaultWalkState(PathTracker.State.OK_INTERRUPTIBLE);
      huntMachine.animate(0.90f, context);
      huntMachine.animate(1.15f, context);
      assertSame(hunting, huntMachine.getCurrentState());
      assertTrue(huntMachine.isMoving());

      // Walk finishes while still out of range -> Hunting gives up on second decide
      context.setDefaultWalkState(PathTracker.State.DONE);
      huntMachine.animate(0.02f, context);
      assertSame(mobileIdle, huntMachine.getCurrentState());
    } finally {
      if (!enemy.isDead()) {
        enemy.remove();
      }
    }
  }

  @Test
  void testWalkScanAttackWithPendingOrderPreservesPendingFallback() {
    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    Unit enemy = spawnUnit(nativePlayer, 18, 18, UnitType.WARRIOR_ROCK);
    try {
      // 1. Modern mode: explicit latched pending order takes precedence over stale walk scan
      context.setLegacyDeterministic(false);
      context.setGridPosition(15, 15);
      context.setAttackRange(2f);
      context.enqueueWalkSteps(PathTracker.State.OK, PathTracker.State.OK_INTERRUPTIBLE);

      Walking modernWalk = new Walking(new LandscapeTarget(25, 25), true, idle);
      UnitStateMachine modernMachine = new UnitStateMachine(modernWalk, context);
      modernMachine.animate(0.02f, context);
      assertEquals(UnitState.StepStatus.UNINTERRUPTIBLE, modernMachine.getLastStepStatus());

      Walking modernPending = new Walking(new LandscapeTarget(30, 30), false, idle);
      modernMachine.requestOrder(modernPending, context);
      context.setScannedEnemy(enemy);
      modernMachine.animate(0.02f, context);
      assertSame(modernPending, modernMachine.getCurrentState());
      assertNull(modernMachine.getPendingState());
      assertFalse(modernMachine.isShouldDecide());

      // 2. Legacy deterministic mode: Hunting wraps pendingState and resets its give-up latch
      FakeUnitContext legacyContext = new FakeUnitContext();
      legacyContext.setLegacyDeterministic(true);
      legacyContext.setGridPosition(15, 15);
      legacyContext.setAttackRange(2f);
      legacyContext.enqueueWalkSteps(PathTracker.State.OK, PathTracker.State.OK_INTERRUPTIBLE);

      Walking walkOrder = new Walking(new LandscapeTarget(25, 25), true, idle);
      UnitStateMachine machine = new UnitStateMachine(walkOrder, legacyContext);
      machine.animate(0.02f, legacyContext);
      assertEquals(UnitState.StepStatus.UNINTERRUPTIBLE, machine.getLastStepStatus());

      Walking pendingWalk = new Walking(new LandscapeTarget(30, 30), true, walkOrder);
      pendingWalk.decide(legacyContext);
      machine.pushOrder(pendingWalk, legacyContext);
      assertTrue(machine.isShouldDecide());
      assertSame(pendingWalk, machine.getPendingState());

      legacyContext.setScannedEnemy(enemy);
      machine.animate(0.02f, legacyContext);

      Hunting hunting = assertInstanceOf(Hunting.class, machine.getCurrentState());
      assertSame(pendingWalk, hunting.getFallbackState());
      assertNull(machine.getPendingState());
      assertFalse(machine.isShouldDecide());

      // Enemy dies -> pops to pendingWalk (whose give-up latch was reset), not discarded or bypassed
      enemy.hit(10000, 1f, 0f, vikingPlayer);
      legacyContext.setScannedEnemy(null);
      legacyContext.setDefaultWalkState(PathTracker.State.DONE);
      machine.requestDecide(legacyContext);
      assertSame(pendingWalk, machine.getCurrentState());

      // Walk finishes: pendingWalk gives up to walkOrder, whose own give-up latch was NOT reset so it pops to idle
      machine.animate(0.02f, legacyContext);
      assertSame(idle, machine.getCurrentState());
    } finally {
      if (!enemy.isDead()) {
        enemy.remove();
      }
    }
  }

  @Test
  void testCastingMagicStateRestartsPreparationWhenPreemptedByStunAndResumed() {
    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    FakeMagicFactory factory = new FakeMagicFactory(1.0f, 2.0f, 3.0f);
    CastingMagic casting = new CastingMagic(factory, idle);

    UnitStateMachine machine = new UnitStateMachine(casting, context);
    assertEquals(Unit.Animation.MAGIC, context.getLastAnimation());

    // Advance past PREPARING (executeMagic called once), then preempt with Stunned during CASTING
    machine.animate(1.0f, context);
    assertEquals(1, context.getEvents().stream().filter("executeMagic"::equals).count());
    assertFalse(context.getEvents().contains("registerMagic"));

    Stunned stun = Stunned.preempt(0.5f, casting, true);
    machine.forceTransition(stun, context);
    assertSame(stun, machine.getCurrentState());
    machine.animate(0.25f, context);
    assertEquals(Unit.Animation.IDLING, context.getLastAnimation());

    // Expire stun -> CastingMagic resumes and must restart from PREPARING and MAGIC animation
    context.clearEvents();
    machine.animate(0.25f, context);
    assertSame(casting, machine.getCurrentState());
    assertEquals(Unit.Animation.MAGIC, context.getLastAnimation());
    assertFalse(context.getEvents().contains("executeMagic"));
    assertFalse(context.getEvents().contains("registerMagic"));

    // Must wait full PREPARING delay before second executeMagic
    machine.animate(0.5f, context);
    assertFalse(context.getEvents().contains("executeMagic"));
    machine.animate(0.5f, context);
    assertEquals(1, context.getEvents().stream().filter("executeMagic"::equals).count());
  }

  @Test
  void testPendingOrderQueryParityForStunAndAttack() {
    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    Unit enemy = spawnUnit(nativePlayer, 16, 15, UnitType.WARRIOR_ROCK);
    try {
      context.setGridPosition(15, 15);
      context.setAttackRange(2f);

      // 1. Stunned state with pending non-stun order: isStunned() reflects active-or-pending state (false)
      Stunned stun = new Stunned(1.0f, idle);
      UnitStateMachine stunMachine = new UnitStateMachine(stun, context);
      stunMachine.animate(0.25f, context);
      assertTrue(stunMachine.isStunned());
      assertEquals(0.75f, stunMachine.getStunTimeLeft(), 1e-6f);

      Walking pendingWalk = new Walking(new LandscapeTarget(20, 20), false, idle);
      stunMachine.requestOrder(pendingWalk, context);
      assertSame(pendingWalk, stunMachine.getPendingState());
      assertFalse(
          stunMachine.isStunned(),
          "Pending non-stun order overrides isStunned() just like legacy getCurrentController()");
      assertEquals(0f, stunMachine.getStunTimeLeft(), 1e-6f);

      // 2. AttackThrowPhase active with pending idle/walk order: isAttacking() remains true
      Hunting hunt = new Hunting(enemy, idle);
      UnitStateMachine attackMachine = new UnitStateMachine(hunt, context);
      attackMachine.animate(0.1f, context);
      assertEquals(UnitState.StepStatus.UNINTERRUPTIBLE, attackMachine.getLastStepStatus());
      attackMachine.requestOrder(new Walking(new LandscapeTarget(20, 20), false, idle), context);
      assertTrue(
          attackMachine.isAttacking(),
          "Active AttackThrowPhase keeps isAttacking() true even when a non-attack order is pending");
      assertEquals(EntityActivity.ACTIVE, attackMachine.getActivity());
    } finally {
      if (!enemy.isDead()) {
        enemy.remove();
      }
    }
  }

  @Test
  void testHarvestingStateFinderWalkSwingTimingAndExitConditions() {
    TreeSupply tree = spawnTree(20, 20);
    // Advance tree to 9 hits so the first swing harvests 1 wood
    for (int i = 0; i < Supply.HITS_PER_HARVEST - 1; i++) {
      assertFalse(tree.hit());
    }

    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    Gathering gatheringFallback = new Gathering(null, SupplyType.WOOD, idle);

    context.setHasSupplyContainer(true);
    context.setMaxCarriedSupplyCount(1);
    context.setWeaponSecondsPerRelease(0.6f);
    context.setLegacyDeterministic(true);
    context.setRandomSeed(777L);
    Random mirror = new Random(777L);

    context.setGridPosition(10, 10);
    context.setFoundSupply(tree);
    context.setDefaultWalkState(PathTracker.State.OK_INTERRUPTIBLE);

    Harvesting harvesting = new Harvesting(null, SupplyType.WOOD, gatheringFallback);
    UnitStateMachine machine = new UnitStateMachine(harvesting, context);

    // Initial decide walks via supply finder and delegates inspection queries to fallbackState
    assertSame(harvesting, machine.getCurrentState());
    assertTrue(machine.isMoving());
    assertFalse(machine.isBlocking());
    assertEquals(EntityActivity.GATHERING, machine.getActivity());
    assertEquals(SupplyType.WOOD, machine.getGatheringSupplyType());
    assertTrue(context.getEvents().contains("initSupplyFinderWalk:WOOD"));

    // Arrive adjacent to tree and finish walk
    context.setGridPosition(21, 20);
    context.setDefaultWalkState(PathTracker.State.DONE);
    machine.animate(0.02f, context);

    assertSame(harvesting, machine.getCurrentState());
    assertFalse(machine.isMoving());
    assertTrue(machine.isBlocking());
    assertEquals(Unit.Animation.THROWING, context.getLastAnimation());
    assertEquals(1f, context.getLastAnimationSpeed(), 1e-6f);

    // Before release threshold (0.6f): no swing event yet
    context.clearEvents();
    machine.animate(0.50f, context);
    assertFalse(context.getEvents().contains("onHarvestSwing:WOOD"));
    assertEquals(0, context.getCarriedSupplyCount());

    // Cross 0.6f release point -> swing harvests 1 wood (now full), consumes nextInt(6)
    machine.animate(0.20f, context);
    assertTrue(context.getEvents().contains("onHarvestSwing:WOOD"));
    assertTrue(context.getEvents().contains("harvested:WOOD"));
    assertEquals(1, context.getCarriedSupplyCount());
    mirror.nextInt(6);
    assertEquals(mirror.nextFloat(), context.nextRandomFloat(), 1e-6f);

    // Still in cycle until animTime > 1.0f
    assertSame(harvesting, machine.getCurrentState());
    assertEquals(UnitState.StepStatus.INTERRUPTIBLE, machine.getLastStepStatus());

    // Complete 1.0s cycle -> isSupplyFull() returns DONE -> decide() transitions to fallbackState
    machine.animate(0.35f, context);
    assertSame(
        gatheringFallback,
        machine.getCurrentState(),
        "Gathering fallback should transition to dropoff walk, not stay in Harvesting");
    assertTrue(machine.isMoving());
  }

  @Test
  void testHarvestingStatePopsOnEmptySupplyAndDelegatesToPlainIdle() {
    RockSupply rock =
        new RockSupply(world, 25, 25, UnitGrid.coordinateFromGrid(25), UnitGrid.coordinateFromGrid(25), true);
    // Leave only 1 hit before total depletion (10 * 10 - 1 = 99 hits)
    for (int i = 0; i < 99; i++) {
      rock.hit();
    }
    assertFalse(rock.isEmpty());

    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    Harvesting harvesting = new Harvesting(rock, SupplyType.ROCK, idle);
    context.setHasSupplyContainer(true);
    context.setMaxCarriedSupplyCount(5);
    context.setWeaponSecondsPerRelease(0.5f);
    context.setGridPosition(26, 25);

    UnitStateMachine machine = new UnitStateMachine(harvesting, context);
    assertSame(harvesting, machine.getCurrentState());
    assertEquals(EntityActivity.IDLE, machine.getActivity());
    assertNull(machine.getGatheringSupplyType());

    // Swing empties the rock; next decide cannot find another supply and gives up after failed walk
    machine.animate(0.6f, context);
    assertTrue(rock.isEmpty());
    assertEquals(1, context.getCarriedSupplyCount());
    assertEquals(SupplyType.ROCK, context.getCarriedSupplyType());

    context.setFoundSupply(null);
    context.setDefaultWalkState(PathTracker.State.DONE);
    machine.animate(0.45f, context);
    // After cycle finishes with empty supply, decide starts a supply-finder walk (give-up counter was reset)
    assertSame(harvesting, machine.getCurrentState());
    assertTrue(machine.isMoving());

    // Walk completes with no found supply -> gives up and pops to idle
    machine.animate(0.02f, context);
    assertSame(idle, machine.getCurrentState());
  }

  @Test
  void testGatheringStateFullLoopDropoffOverflowAndGiveUpTransfer() {
    TreeSupply tree = spawnTree(15, 15);
    for (int i = 0; i < Supply.HITS_PER_HARVEST - 1; i++) {
      tree.hit();
    }
    Building armory = placeBuiltBuilding(vikingPlayer, BuildingType.ARMORY, 30, 30);

    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    Gathering gathering = new Gathering(tree, SupplyType.WOOD, idle);

    context.setHasSupplyContainer(true);
    context.setMaxCarriedSupplyCount(1);
    context.setWeaponSecondsPerRelease(0.5f);
    context.setGridPosition(16, 15);

    // 1. Adjacent to tree -> immediate zero-tick transition to Harvesting
    UnitStateMachine machine = new UnitStateMachine(gathering, context);
    assertInstanceOf(Harvesting.class, machine.getCurrentState());
    assertEquals(EntityActivity.GATHERING, machine.getActivity());
    assertEquals(SupplyType.WOOD, machine.getGatheringSupplyType());

    // Complete 1 harvest cycle -> full wood -> pops back to Gathering -> starts dropoff walk
    context.setFoundSupplyBuilding(armory);
    context.setDefaultWalkState(PathTracker.State.OK_INTERRUPTIBLE);
    machine.animate(0.6f, context);
    machine.animate(0.45f, context);

    assertSame(gathering, machine.getCurrentState());
    assertTrue(machine.isMoving());
    assertTrue(context.getEvents().contains("initSupplyBuildingFinderWalk"));

    // Arrive adjacent to armory -> deposit succeeds (depositFullResult == false) -> walks back to tree
    context.setGridPosition(31, 30);
    context.setDefaultWalkState(PathTracker.State.DONE);
    context.setFoundSupply(tree);
    context.clearEvents();
    machine.animate(0.02f, context);

    assertTrue(context.getEvents().contains("depositSupplyIntoBuilding"));
    assertEquals(0, context.getCarriedSupplyCount());
    assertSame(gathering, machine.getCurrentState());
    assertTrue(machine.isMoving());

    // Arrive back at tree -> transitions to Harvesting again
    context.setGridPosition(16, 15);
    machine.animate(0.02f, context);
    assertInstanceOf(Harvesting.class, machine.getCurrentState());

    // 2. When deposit leaves remaining supplies in hand, transition to EnteringBuilding
    context.setCarriedSupplyType(SupplyType.WOOD);
    context.setCarriedSupplyCount(1);
    context.setDepositFullResult(true);
    context.setGridPosition(31, 30);
    machine.requestOrder(new Gathering(tree, SupplyType.WOOD, idle), context);

    // First decide on new order initiates dropoff walk because buildingTrackerActive is fresh
    assertTrue(machine.isMoving());
    machine.animate(0.02f, context);
    // Now arrived at armory with depositFullResult == true -> transitions to EnteringBuilding,
    // which immediately sees armory is adjacent, calls enterBuilding, and pops to idle
    assertTrue(context.getEvents().contains("enterBuilding"));
    assertSame(idle, machine.getCurrentState());

    // 3. When Phase.HARVEST gives up on a non-null unreachable supply, fall back to Transporting
    TreeSupply unreachableTree = spawnTree(45, 45);
    FakeUnitContext giveUpContext = new FakeUnitContext();
    giveUpContext.setGridPosition(5, 5);
    giveUpContext.setAttackRange(2f);
    giveUpContext.setDefaultWalkState(PathTracker.State.DONE);

    Gathering failingGather = new Gathering(unreachableTree, SupplyType.WOOD, idle);
    UnitStateMachine failMachine = new UnitStateMachine(failingGather, giveUpContext);
    assertSame(failingGather, failMachine.getCurrentState());
    assertTrue(failMachine.isMoving());

    // Walk completes while still out of range -> Phase.HARVEST gives up -> Transporting walks to building
    failMachine.animate(0.02f, giveUpContext);
    assertInstanceOf(Transporting.class, failMachine.getCurrentState());
    assertTrue(failMachine.isMoving());
    assertTrue(giveUpContext.getEvents().contains("initSupplyBuildingFinderWalk"));
  }

  @Test
  void testPlacingBuildingToRepairingToHarvestingZeroTickCascade() {
    TreeSupply tree = spawnTree(25, 16);
    for (int i = 0; i < Supply.HITS_PER_HARVEST - 1; i++) {
      tree.hit();
    }
    Building tower =
        new Building(vikingPlayer, vikingPlayer.getRaceInfo().getBuildingTemplate(BuildingType.TOWER), 16, 16);
    assertFalse(tower.isPlaced());

    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    PlacingBuilding placing = new PlacingBuilding(tower, idle);

    // Position peon within tower.getSize() (~2.93) with 0 carried wood
    context.setGridPosition(18, 16);
    context.setBuildingPlacementLegal(true);
    context.setHasSupplyContainer(true);
    context.setMaxCarriedSupplyCount(1);
    context.setWeaponSecondsPerRelease(0.5f);
    context.setFoundSupply(tree);
    context.setDefaultWalkState(PathTracker.State.OK_INTERRUPTIBLE);

    UnitStateMachine machine = new UnitStateMachine(placing, context);

    assertTrue(tower.isPlaced(), "PlacingBuilding must place building in 0 ticks");
    assertTrue(context.getEvents().contains("placeBuilding"));
    assertInstanceOf(Harvesting.class, machine.getCurrentState());
    assertTrue(machine.isMoving());
    assertTrue(context.getEvents().contains("initSupplyFinderWalk:WOOD"));
    assertEquals(EntityActivity.OTHER, machine.getActivity());
    assertFalse(machine.isPlacingBuilding());

    // Harvest 1 wood from tree -> pops back to Repairing walking back to tower
    context.setGridPosition(26, 16);
    context.setDefaultWalkState(PathTracker.State.DONE);
    machine.animate(0.02f, context);
    context.setDefaultWalkState(PathTracker.State.OK_INTERRUPTIBLE);
    machine.animate(0.6f, context);
    machine.animate(0.45f, context);

    Repairing repairing = assertInstanceOf(Repairing.class, machine.getCurrentState());
    assertSame(tower, repairing.getBuilding());
    assertTrue(machine.isMoving());
  }

  @Test
  void testPlacingBuildingWalksAndHandlesIllegalOrDeadSites() {
    Building tower =
        new Building(vikingPlayer, vikingPlayer.getRaceInfo().getBuildingTemplate(BuildingType.TOWER), 20, 20);
    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    PlacingBuilding placing = new PlacingBuilding(tower, idle);

    // Out of range -> walks with PLACING_BUILDING activity
    context.setGridPosition(5, 5);
    UnitStateMachine machine = new UnitStateMachine(placing, context);
    assertSame(placing, machine.getCurrentState());
    assertTrue(machine.isPlacingBuilding());
    assertTrue(machine.isMoving());
    assertEquals(EntityActivity.PLACING_BUILDING, machine.getActivity());
    assertSame(tower, placing.getBuilding());

    // Arrive at site where placement is illegal -> pops to fallback without placing
    context.setGridPosition(21, 20);
    context.setBuildingPlacementLegal(false);
    context.setDefaultWalkState(PathTracker.State.DONE);
    machine.animate(0.02f, context);
    assertFalse(tower.isPlaced());
    assertSame(idle, machine.getCurrentState());
  }

  @Test
  void testRepairingStateFiveSwingsPerWoodAndPostRepairBranching() {
    // 1. Armory repair: consumes 1 wood upfront, performs 5 repair points, and enters Armory when repaired
    Building armory =
        new Building(vikingPlayer, vikingPlayer.getRaceInfo().getBuildingTemplate(BuildingType.ARMORY), 20, 20);
    armory.place();
    int maxHp = armory.getTemplate().getMaxHitPoints();
    armory.repair(maxHp - 8); // 7 HP remaining to full
    int hpBefore = armory.getHitPoints();

    Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
    Repairing repairArmory = new Repairing(armory, idle);

    context.setHasSupplyContainer(true);
    context.setMaxCarriedSupplyCount(5);
    context.setCarriedSupplyType(SupplyType.WOOD);
    context.setCarriedSupplyCount(3);
    context.setWeaponSecondsPerRelease(0.5f);
    context.setLegacyDeterministic(true);
    context.setFriendlySupplyContainerBuilding(true);
    context.setGridPosition(21, 20);

    UnitStateMachine machine = new UnitStateMachine(repairArmory, context);
    assertSame(repairArmory, machine.getCurrentState());
    assertTrue(machine.isBlocking());
    assertFalse(machine.isMoving());
    assertEquals(EntityActivity.OTHER, machine.getActivity());
    // Consumed 1 wood immediately upon starting RepairCyclePhase
    assertEquals(2, context.getCarriedSupplyCount());
    assertTrue(context.getEvents().contains("consumeOneWoodForRepair"));

    // Execute 4 full 1-second repair cycles -> still in RepairCyclePhase
    for (int i = 0; i < Repairing.RepairCyclePhase.REPAIRS_PER_SUPPLY - 1; i++) {
      machine.animate(0.6f, context);
      machine.animate(0.45f, context);
      assertSame(repairArmory, machine.getCurrentState());
      assertEquals(hpBefore + i + 1, armory.getHitPoints());
    }

    // 5th repair cycle completes REPAIRS_PER_SUPPLY -> applies 5th point, returns DONE,
    // decide consumes 2nd wood and starts next RepairCyclePhase (1 wood remaining)
    context.clearEvents();
    machine.animate(0.6f, context);
    machine.animate(0.45f, context);
    assertEquals(hpBefore + 5, armory.getHitPoints());
    assertEquals(1, context.getCarriedSupplyCount());
    assertSame(repairArmory, machine.getCurrentState());

    // Next 2 cycles finish the remaining 2 HP; 3rd cycle detects built & undamaged and pops DONE ->
    // Armory is friendly supply container with 1 wood still in hand -> transitions to EnteringBuilding -> idle
    for (int i = 0; i < 3; i++) {
      machine.animate(0.6f, context);
      machine.animate(0.45f, context);
    }
    assertTrue(armory.isBuilt());
    assertFalse(armory.isDamaged());
    assertTrue(context.getEvents().contains("enterBuilding"));
    assertSame(idle, machine.getCurrentState());

    // 2. Tower (non-supply-container) repair: pops directly to fallbackState without entering
    Building tower =
        new Building(vikingPlayer, vikingPlayer.getRaceInfo().getBuildingTemplate(BuildingType.TOWER), 35, 35);
    tower.place();
    tower.repair(tower.getTemplate().getMaxHitPoints() - 2);

    FakeUnitContext towerContext = new FakeUnitContext();
    towerContext.setHasSupplyContainer(true);
    towerContext.setMaxCarriedSupplyCount(5);
    towerContext.setCarriedSupplyType(SupplyType.WOOD);
    towerContext.setCarriedSupplyCount(2);
    towerContext.setWeaponSecondsPerRelease(0.5f);
    towerContext.setFriendlySupplyContainerBuilding(false);
    towerContext.setGridPosition(36, 35);

    UnitStateMachine towerMachine = new UnitStateMachine(new Repairing(tower, idle), towerContext);
    for (int i = 0; i < 2; i++) {
      towerMachine.animate(0.6f, towerContext);
      towerMachine.animate(0.45f, towerContext);
    }
    assertTrue(tower.isBuilt());
    assertFalse(towerContext.getEvents().contains("enterBuilding"));
    assertSame(idle, towerMachine.getCurrentState());
  }

  @Test
  void testEnteringBuildingAndTransferringUnitWithReentrantForceTransition() {
    Building tower = placeBuiltBuilding(vikingPlayer, BuildingType.TOWER, 18, 18);
    Building armory = placeBuiltBuilding(vikingPlayer, BuildingType.ARMORY, 28, 28);

    Idling fallbackIdle = new Idling(AttackScanFilter.UNIT_RANGE, true);

    // 1. EnteringBuilding walks to building and handles re-entrant mount forceTransition
    context.setGridPosition(5, 5);
    context.setDefaultWalkState(PathTracker.State.OK_INTERRUPTIBLE);
    EnteringBuilding entering = new EnteringBuilding(tower, fallbackIdle);
    UnitStateMachine enterMachine = new UnitStateMachine(entering, context);

    assertTrue(enterMachine.isMoving());
    assertEquals(EntityActivity.OTHER, enterMachine.getActivity());

    Idling towerIdle = new Idling(AttackScanFilter.TOWER_RANGE, false);
    context.setEnterBuildingHook(_ -> enterMachine.forceTransition(towerIdle, context));
    context.setGridPosition(19, 18);
    context.setDefaultWalkState(PathTracker.State.DONE);
    enterMachine.animate(0.02f, context);

    assertTrue(context.getEvents().contains("enterBuilding"));
    assertSame(
        towerIdle,
        enterMachine.getCurrentState(),
        "Re-entrant forceTransition during enterBuilding must not be overwritten by fallbackState");

    // 2. Transporting locates supply building, enters without deposit, and honors re-entrant forceTransition
    FakeUnitContext transferContext = new FakeUnitContext();
    transferContext.setGridPosition(5, 5);
    transferContext.setFoundSupplyBuilding(armory);
    transferContext.setDefaultWalkState(PathTracker.State.OK_INTERRUPTIBLE);

    Transporting transferring = new Transporting(fallbackIdle);
    UnitStateMachine transferMachine = new UnitStateMachine(transferring, transferContext);
    assertTrue(transferMachine.isMoving());
    assertTrue(transferContext.getEvents().contains("initSupplyBuildingFinderWalk"));

    Dying terminalDying = new Dying();
    transferContext.setEnterBuildingHook(_ -> transferMachine.forceTransition(terminalDying, transferContext));
    transferContext.setGridPosition(29, 28);
    transferContext.setDefaultWalkState(PathTracker.State.DONE);
    transferMachine.animate(0.02f, transferContext);

    assertTrue(transferContext.getEvents().contains("enterBuildingWithoutDeposit"));
    assertFalse(transferContext.getEvents().contains("enterBuilding"));
    assertSame(
        terminalDying,
        transferMachine.getCurrentState(),
        "Re-entrant forceTransition during enterBuildingWithoutDeposit must survive decide()");
  }

  @Test
  void testGatheringRemainingSupplyResolvesFallbackBeforeEnteringBuilding() {
    Building armory = placeBuiltBuilding(vikingPlayer, BuildingType.ARMORY, 30, 30);
    TreeSupply tree = spawnTree(15, 15);
    Unit enemy = spawnUnit(nativePlayer, 32, 30, UnitType.PEON);
    try {
      Idling idle = new Idling(AttackScanFilter.UNIT_RANGE, true);
      Hunting fallbackHunt = new Hunting(enemy, idle);

      context.setGridPosition(31, 30);
      context.setAttackRange(2f);
      context.setCarriedSupplyType(SupplyType.WOOD);
      context.setCarriedSupplyCount(1);
      context.setDepositFullResult(true);
      context.setFoundSupplyBuilding(armory);
      context.setDefaultWalkState(PathTracker.State.DONE);

      // Prevent enterBuilding from killing the unit so we can observe fallback resolution and stack wiring
      context.setCanEnterBuildingResult(false);

      Gathering gathering = new Gathering(tree, SupplyType.WOOD, fallbackHunt);
      UnitStateMachine machine = new UnitStateMachine(gathering, context);
      context.clearEvents();

      machine.animate(0.02f, context);

      assertTrue(context.getEvents().contains("depositSupplyIntoBuilding"));
      // PopAndPush resolved fallbackHunt (which started AttackThrowPhase -> THROWING animation)
      // before pushing EnteringBuilding (which failed canEnter and popped back to fallbackHunt)
      assertEquals(
          Unit.Animation.THROWING,
          context.getLastAnimation(),
          "Fallback Hunting.decide() must run before EnteringBuilding");
      assertSame(fallbackHunt, machine.getCurrentState());
      assertTrue(machine.isAttacking());
    } finally {
      if (!enemy.isDead()) {
        enemy.remove();
      }
    }
  }

  private static final class FakeMagicFactory implements MagicFactory {
    private final float secondsPerInit;
    private final float secondsPerRelease;
    private final float secondsPerAnim;

    FakeMagicFactory(float secondsPerInit, float secondsPerRelease, float secondsPerAnim) {
      this.secondsPerInit = secondsPerInit;
      this.secondsPerRelease = secondsPerRelease;
      this.secondsPerAnim = secondsPerAnim;
    }

    @Override
    public float getHitRadius() {
      return 5f;
    }

    @Override
    public float getSecondsPerAnim() {
      return secondsPerAnim;
    }

    @Override
    public float getSecondsPerInit() {
      return secondsPerInit;
    }

    @Override
    public float getSecondsPerRelease() {
      return secondsPerRelease;
    }

    @Override
    public Magic execute(Unit src) {
      throw new UnsupportedOperationException("FakeUnitContext handles executeMagic directly");
    }
  }

  private static final class FakeMovable implements Movable {
    private int gridX;
    private int gridY;
    private boolean markedBlocking;

    FakeMovable(int gridX, int gridY) {
      this.gridX = gridX;
      this.gridY = gridY;
    }

    @Override
    public boolean isMoving() {
      return false;
    }

    @Override
    public int getGridX() {
      return gridX;
    }

    @Override
    public int getGridY() {
      return gridY;
    }

    @Override
    public void setPosition(float x, float y) {}

    @Override
    public void setDirection(float dirX, float dirY) {}

    @Override
    public float getPositionX() {
      return gridX * 2f + 1f;
    }

    @Override
    public float getPositionY() {
      return gridY * 2f + 1f;
    }

    @Override
    public PathTracker getTracker() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void free() {}

    @Override
    public void setGridPosition(int gridX, int gridY) {
      this.gridX = gridX;
      this.gridY = gridY;
    }

    @Override
    public void occupy() {}

    @Override
    public void markBlocking() {
      this.markedBlocking = true;
    }

    boolean isMarkedBlocking() {
      return markedBlocking;
    }

    @Override
    public boolean isDead() {
      return false;
    }
  }
}
