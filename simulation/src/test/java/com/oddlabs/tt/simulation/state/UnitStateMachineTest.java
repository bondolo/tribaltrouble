package com.oddlabs.tt.simulation.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oddlabs.tt.simulation.model.EntityActivity;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@NullMarked
final class UnitStateMachineTest {
  private enum TestPhase {
    HARVEST,
    DROPOFF
  }

  private FakeUnitContext context;

  @BeforeEach
  void setUp() {
    context = new FakeUnitContext();
  }

  @Test
  void testZeroTickMultiHopTransitionCascadeOnRequestOrder() {
    List<String> log = new ArrayList<>();
    UnitState stateD = new RecordingLeafState("D", UnitState.StepStatus.INTERRUPTIBLE, log);
    UnitState stateC = new HopState("C", stateD, log);
    UnitState stateB = new HopState("B", stateC, log);
    UnitState stateA = new HopState("A", stateB, log);
    UnitState initial = new RecordingLeafState("Initial", UnitState.StepStatus.INTERRUPTIBLE, log);

    UnitStateMachine machine = new UnitStateMachine(initial);
    machine.requestOrder(stateA, context);

    assertSame(stateD, machine.getCurrentState());
    assertEquals(List.of("A:decide->B", "B:decide->C", "C:decide->D", "D:decide->stay"), log);
    assertFalse(machine.isShouldDecide());
    assertNull(machine.getPendingState());
  }

  @Test
  void testZeroTickMultiHopTransitionCascadeWhenLeafDone() {
    List<String> log = new ArrayList<>();
    UnitState stateD = new RecordingLeafState("D", UnitState.StepStatus.INTERRUPTIBLE, log);
    UnitState stateC = new HopState("C", stateD, log);
    UnitState stateB = new HopState("B", stateC, log);
    UnitState leaf = new ScriptedTickState("Leaf", List.of(UnitState.TickResult.done(stateB)), log);

    UnitStateMachine machine = new UnitStateMachine(leaf, context);
    log.clear();

    machine.animate(0.02f, context);

    assertSame(stateD, machine.getCurrentState());
    assertEquals(List.of("Leaf:tick", "B:decide->C", "C:decide->D", "D:decide->stay"), log);
  }

  @Test
  void testUninterruptibleDeferralLatchReleasesOnInterruptible() {
    List<String> log = new ArrayList<>();
    UnitState uninterruptibleLeaf =
        new ScriptedTickState(
            "Attack",
            List.of(
                UnitState.TickResult.UNINTERRUPTIBLE,
                UnitState.TickResult.UNINTERRUPTIBLE,
                UnitState.TickResult.INTERRUPTIBLE),
            log);
    UnitState newOrder = new RecordingLeafState("NewOrder", UnitState.StepStatus.INTERRUPTIBLE, log);

    UnitStateMachine machine = new UnitStateMachine(uninterruptibleLeaf, context);
    machine.animate(0.02f, context);
    assertEquals(UnitState.StepStatus.UNINTERRUPTIBLE, machine.getLastStepStatus());

    machine.requestOrder(newOrder, context);
    assertTrue(machine.isShouldDecide());
    assertSame(newOrder, machine.getPendingState());
    assertSame(uninterruptibleLeaf, machine.getCurrentState());

    // Still UNINTERRUPTIBLE on second tick: order must remain latched
    machine.animate(0.02f, context);
    assertTrue(machine.isShouldDecide());
    assertSame(newOrder, machine.getPendingState());
    assertSame(uninterruptibleLeaf, machine.getCurrentState());

    // Third tick returns INTERRUPTIBLE: latched order must activate immediately
    machine.animate(0.02f, context);
    assertFalse(machine.isShouldDecide());
    assertNull(machine.getPendingState());
    assertSame(newOrder, machine.getCurrentState());
  }

  @Test
  void testUninterruptibleDeferralLatchReleasesOnDone() {
    List<String> log = new ArrayList<>();
    UnitState uninterruptibleLeaf =
        new ScriptedTickState("Step", List.of(UnitState.TickResult.UNINTERRUPTIBLE, UnitState.TickResult.DONE), log);
    UnitState newOrder = new RecordingLeafState("NewOrder", UnitState.StepStatus.INTERRUPTIBLE, log);

    UnitStateMachine machine = new UnitStateMachine(uninterruptibleLeaf, context);
    machine.animate(0.02f, context);

    machine.requestOrder(newOrder, context);
    assertTrue(machine.isShouldDecide());
    assertSame(uninterruptibleLeaf, machine.getCurrentState());

    machine.animate(0.02f, context);
    assertFalse(machine.isShouldDecide());
    assertNull(machine.getPendingState());
    assertSame(newOrder, machine.getCurrentState());
  }

  @Test
  void testForceTransitionImmediatePreemption() {
    List<String> log = new ArrayList<>();
    UnitState magicState = new ScriptedTickState("Magic", List.of(UnitState.TickResult.UNINTERRUPTIBLE), log);
    UnitState queuedOrder = new RecordingLeafState("QueuedWalk", UnitState.StepStatus.INTERRUPTIBLE, log);
    UnitState stunnedTarget = new RecordingLeafState("Stunned", UnitState.StepStatus.UNINTERRUPTIBLE, log);
    UnitState stunHop = new HopState("StunHop", stunnedTarget, log);

    UnitStateMachine machine = new UnitStateMachine(magicState, context);
    machine.animate(0.02f, context);
    machine.requestOrder(queuedOrder, context);

    assertTrue(machine.isShouldDecide());
    assertSame(queuedOrder, machine.getPendingState());
    assertSame(magicState, machine.getCurrentState());

    log.clear();
    machine.forceTransition(stunHop, context);

    assertSame(stunnedTarget, machine.getCurrentState());
    assertFalse(machine.isShouldDecide());
    assertNull(machine.getPendingState());
    assertEquals(List.of("StunHop:decide->Stunned", "Stunned:decide->stay"), log);
  }

  @Test
  void testReentrantForceTransitionInsideDecideIsPreserved() {
    List<String> log = new ArrayList<>();
    UnitState mountedTerminal = new RecordingLeafState("MountedIdle", UnitState.StepStatus.INTERRUPTIBLE, log);
    UnitState fallbackState = new RecordingLeafState("Fallback", UnitState.StepStatus.INTERRUPTIBLE, log);
    UnitStateMachine[] smRef = new UnitStateMachine[1];

    UnitState enteringState =
        new TestUnitState() {
          @Override
          public Transition decide(UnitContext ctx) {
            log.add("Enter:decide");
            ctx.switchToIdleAnimation();
            ctx.setOffsetZ(0f);
            smRef[0].forceTransition(mountedTerminal, ctx);
            // Even if this returns Transition.to(fallbackState), the outer loop must ignore it
            return Transition.to(fallbackState);
          }

          @Override
          public TickResult tick(float dt, UnitContext ctx) {
            return TickResult.DONE;
          }

          @Override
          public EntityActivity getActivity() {
            return EntityActivity.OTHER;
          }

          @Override
          public boolean isBlocking() {
            return true;
          }
        };

    UnitStateMachine machine =
        new UnitStateMachine(new RecordingLeafState("Idle", UnitState.StepStatus.INTERRUPTIBLE, log));
    smRef[0] = machine;

    machine.requestOrder(enteringState, context);

    assertSame(mountedTerminal, machine.getCurrentState());
    assertEquals(List.of("switchToIdleAnimation", "setOffsetZ:0.0"), context.getEvents());
    assertFalse(log.contains("Fallback:decide->stay"));
  }

  @Test
  void testReentrantForceTransitionInsideTickIsPreserved() {
    List<String> log = new ArrayList<>();
    UnitState dyingTerminal = new RecordingLeafState("Dying", UnitState.StepStatus.UNINTERRUPTIBLE, log);
    UnitState nextFromTick = new RecordingLeafState("NextFromTick", UnitState.StepStatus.INTERRUPTIBLE, log);
    UnitStateMachine[] smRef = new UnitStateMachine[1];

    UnitState tickingState =
        new TestUnitState() {
          @Override
          public Transition decide(UnitContext ctx) {
            return Transition.stay();
          }

          @Override
          public TickResult tick(float dt, UnitContext ctx) {
            log.add("Tick:beforeForce");
            smRef[0].forceTransition(dyingTerminal, ctx);
            return TickResult.done(nextFromTick);
          }

          @Override
          public EntityActivity getActivity() {
            return EntityActivity.ACTIVE;
          }

          @Override
          public boolean isBlocking() {
            return true;
          }
        };

    UnitStateMachine machine = new UnitStateMachine(tickingState, context);
    smRef[0] = machine;

    machine.animate(0.02f, context);

    assertSame(dyingTerminal, machine.getCurrentState());
    assertEquals(UnitState.StepStatus.INTERRUPTIBLE, machine.getLastStepStatus());
    assertFalse(machine.isShouldDecide());
    assertFalse(log.contains("NextFromTick:decide->stay"));
  }

  @Test
  void testZeroTickLoopGuardThrows() {
    List<String> log = new ArrayList<>();
    UnitState[] cycle = new UnitState[1];
    cycle[0] =
        new TestUnitState() {
          @Override
          public Transition decide(UnitContext ctx) {
            return Transition.to(cycle[0]);
          }

          @Override
          public TickResult tick(float dt, UnitContext ctx) {
            return TickResult.DONE;
          }

          @Override
          public EntityActivity getActivity() {
            return EntityActivity.OTHER;
          }

          @Override
          public boolean isBlocking() {
            return true;
          }
        };

    UnitStateMachine machine =
        new UnitStateMachine(new RecordingLeafState("Idle", UnitState.StepStatus.INTERRUPTIBLE, log));
    assertThrows(IllegalStateException.class, () -> machine.requestOrder(cycle[0], context));
  }

  @Test
  void testGiveUpTrackerRetryParity() {
    GiveUpTracker<TestPhase> tracker = new GiveUpTracker<>(TestPhase.class);

    assertFalse(tracker.shouldGiveUp(TestPhase.HARVEST));
    assertTrue(tracker.shouldGiveUp(TestPhase.HARVEST));

    assertFalse(tracker.shouldGiveUp(TestPhase.DROPOFF));
    assertTrue(tracker.shouldGiveUp(TestPhase.DROPOFF));

    tracker.reset(TestPhase.HARVEST);
    assertFalse(tracker.shouldGiveUp(TestPhase.HARVEST));
    assertTrue(tracker.shouldGiveUp(TestPhase.DROPOFF));

    tracker.resetAll();
    assertFalse(tracker.shouldGiveUp(TestPhase.HARVEST));
    assertFalse(tracker.shouldGiveUp(TestPhase.DROPOFF));

    GiveUpTracker.Single single = new GiveUpTracker.Single();
    assertFalse(single.shouldGiveUp());
    assertTrue(single.shouldGiveUp());
    single.reset();
    assertFalse(single.shouldGiveUp());
  }

  @Test
  void testReplaceDefaultAndRequestOrderDefersWhileUninterruptible() {
    List<String> log = new ArrayList<>();
    ScriptedTickState active =
        new ScriptedTickState("Active", List.of(UnitState.TickResult.UNINTERRUPTIBLE, UnitState.TickResult.DONE), log);
    RecordingLeafState newDefault = new RecordingLeafState("NewDefault", UnitState.StepStatus.INTERRUPTIBLE, log);

    UnitStateMachine machine = new UnitStateMachine(active, context);
    machine.animate(0.02f, context);
    assertEquals(UnitState.StepStatus.UNINTERRUPTIBLE, machine.getLastStepStatus());

    machine.replaceDefaultAndRequestOrder(newDefault, context);
    assertSame(
        active, machine.getCurrentState(), "replaceDefaultAndRequestOrder must defer transition while UNINTERRUPTIBLE");
    assertSame(newDefault, machine.getPendingState());
    assertTrue(machine.isShouldDecide());

    machine.animate(0.02f, context);
    assertSame(newDefault, machine.getCurrentState());
    assertNull(machine.getPendingState());
    assertFalse(machine.isShouldDecide());
  }

  @Test
  void testPopAndPushResolvesFallbackBeforePushingNextOrder() {
    List<String> log = new ArrayList<>();
    RecordingLeafState settledFallback = new RecordingLeafState("Settled", UnitState.StepStatus.INTERRUPTIBLE, log);
    TestUnitState intermediateFallback =
        new TestUnitState() {
          @Override
          public Transition decide(UnitContext ctx) {
            log.add("Intermediate:decide->Settled");
            return Transition.to(settledFallback);
          }

          @Override
          public TickResult tick(float dt, UnitContext ctx) {
            return TickResult.DONE;
          }

          @Override
          public EntityActivity getActivity() {
            return EntityActivity.OTHER;
          }

          @Override
          public boolean isBlocking() {
            return true;
          }
        };
    RecordingLeafState pushed = new RecordingLeafState("Pushed", UnitState.StepStatus.INTERRUPTIBLE, log);
    final UnitState[] observedResolvedFallback = new UnitState[1];
    TestUnitState source =
        new TestUnitState() {
          @Override
          public Transition decide(UnitContext ctx) {
            log.add("Source:decide->PopAndPush");
            return Transition.popAndPush(
                intermediateFallback,
                resolved -> {
                  observedResolvedFallback[0] = resolved;
                  return pushed;
                });
          }

          @Override
          public TickResult tick(float dt, UnitContext ctx) {
            return TickResult.DONE;
          }

          @Override
          public EntityActivity getActivity() {
            return EntityActivity.OTHER;
          }

          @Override
          public boolean isBlocking() {
            return true;
          }
        };

    UnitStateMachine machine = new UnitStateMachine(source, context);
    assertSame(settledFallback, observedResolvedFallback[0]);
    assertSame(pushed, machine.getCurrentState());
    assertEquals(
        List.of(
            "Source:decide->PopAndPush", "Intermediate:decide->Settled", "Settled:decide->stay", "Pushed:decide->stay"),
        log);
  }

  private static final class HopState implements TestUnitState {
    private final String name;
    private final UnitState target;
    private final List<String> log;

    HopState(String name, UnitState target, List<String> log) {
      this.name = name;
      this.target = target;
      this.log = log;
    }

    @Override
    public Transition decide(UnitContext context) {
      log.add(name + ":decide->" + targetName());
      return Transition.to(target);
    }

    private String targetName() {
      return switch (target) {
        case HopState h -> h.name;
        case RecordingLeafState r -> r.name;
        case ScriptedTickState s -> s.name;
        default -> target.toString();
      };
    }

    @Override
    public TickResult tick(float dt, UnitContext context) {
      return TickResult.DONE;
    }

    @Override
    public EntityActivity getActivity() {
      return EntityActivity.OTHER;
    }

    @Override
    public boolean isBlocking() {
      return true;
    }
  }

  private static final class RecordingLeafState implements TestUnitState {
    private final String name;
    private final StepStatus tickStatus;
    private final List<String> log;

    RecordingLeafState(String name, StepStatus tickStatus, List<String> log) {
      this.name = name;
      this.tickStatus = tickStatus;
      this.log = log;
    }

    @Override
    public Transition decide(UnitContext context) {
      log.add(name + ":decide->stay");
      return Transition.stay();
    }

    @Override
    public TickResult tick(float dt, UnitContext context) {
      log.add(name + ":tick");
      return TickResult.of(tickStatus);
    }

    @Override
    public EntityActivity getActivity() {
      return EntityActivity.IDLE;
    }

    @Override
    public boolean isBlocking() {
      return true;
    }
  }

  private static final class ScriptedTickState implements TestUnitState {
    private final String name;
    private final List<TickResult> results;
    private final List<String> log;
    private int index;

    ScriptedTickState(String name, List<TickResult> results, List<String> log) {
      this.name = name;
      this.results = results;
      this.log = log;
    }

    @Override
    public Transition decide(UnitContext context) {
      log.add(name + ":decide->stay");
      return Transition.stay();
    }

    @Override
    public TickResult tick(float dt, UnitContext context) {
      log.add(name + ":tick");
      TickResult result = results.get(Math.min(index, results.size() - 1));
      index++;
      return result;
    }

    @Override
    public EntityActivity getActivity() {
      return EntityActivity.ACTIVE;
    }

    @Override
    public boolean isBlocking() {
      return true;
    }
  }
}
