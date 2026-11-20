package com.oddlabs.tt.simulation.state;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Unified behavioural state contract combining decision transitions and per-tick execution.
public sealed interface UnitState
    permits CastingMagic,
        Defending,
        Dying,
        EnteringBuilding,
        Gathering,
        Harvesting,
        Hunting,
        Idling,
        PlacingBuilding,
        Repairing,
        Stunned,
        Transporting,
        Walking,
        TestUnitState {
  /// Interruptibility classification matching legacy `Behaviour.State` names for checksum parity.
  enum StepStatus {
    UNINTERRUPTIBLE,
    INTERRUPTIBLE,
    DONE
  }

  /// Outcome of a synchronous `decide` evaluation.
  sealed interface Transition permits Transition.Stay, Transition.Walk, Transition.To, Transition.PopAndPush {
    /// Settled on the current state without an immediate transition.
    record Stay() implements Transition {
      public static final Stay INSTANCE = new Stay();
    }

    /// Settled on the current state while activating `WalkingPhase` movement.
    record Walk(boolean scanAttack) implements Transition {
      public static final Walk PASSIVE = new Walk(false);
      public static final Walk AGGRESSIVE = new Walk(true);
    }

    /// Immediate zero-tick transition to `nextState`.
    record To(UnitState nextState) implements Transition {}

    /// Resolves `fallbackState` first (matching legacy `popController(); pushController(...)`) and then pushes the
    /// state produced by `nextFactory` onto the settled fallback state.
    record PopAndPush(UnitState fallbackState, Function<UnitState, UnitState> nextFactory) implements Transition {}

    static Transition stay() {
      return Stay.INSTANCE;
    }

    static Transition walk(boolean scanAttack) {
      return scanAttack ? Walk.AGGRESSIVE : Walk.PASSIVE;
    }

    static Transition to(UnitState nextState) {
      return new To(nextState);
    }

    static Transition popAndPush(UnitState fallbackState, Function<UnitState, UnitState> nextFactory) {
      return new PopAndPush(fallbackState, nextFactory);
    }
  }

  /// Outcome of a single simulation tick on the active state.
  record TickResult(StepStatus status, @Nullable UnitState nextState, boolean idleAnimationAfterDecide) {
    public static final TickResult UNINTERRUPTIBLE = new TickResult(StepStatus.UNINTERRUPTIBLE, null, false);

    public static final TickResult INTERRUPTIBLE = new TickResult(StepStatus.INTERRUPTIBLE, null, false);

    public static final TickResult DONE = new TickResult(StepStatus.DONE, null, false);

    public TickResult(StepStatus status, @Nullable UnitState nextState) {
      this(status, nextState, false);
    }

    public static TickResult of(StepStatus status) {
      return switch (status) {
        case UNINTERRUPTIBLE -> UNINTERRUPTIBLE;
        case INTERRUPTIBLE -> INTERRUPTIBLE;
        case DONE -> DONE;
      };
    }

    public static TickResult of(StepStatus status, UnitState nextState) {
      return new TickResult(status, nextState, false);
    }

    public static TickResult done(UnitState nextState) {
      return of(StepStatus.DONE, nextState);
    }

    public static TickResult interruptible(UnitState nextState) {
      return of(StepStatus.INTERRUPTIBLE, nextState);
    }

    public static TickResult interruptibleWithPostIdleAnimation(UnitState nextState) {
      return new TickResult(StepStatus.INTERRUPTIBLE, nextState, true);
    }
  }

  Transition decide(UnitContext context);

  default TickResult tick(float dt, UnitContext context) {
    return TickResult.DONE;
  }

  default void resetGiveUpCounters() {}

  default @Nullable UnitState getFallbackState() {
    return null;
  }
}
