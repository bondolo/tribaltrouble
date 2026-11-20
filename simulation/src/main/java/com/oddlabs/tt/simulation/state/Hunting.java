package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import org.jspecify.annotations.Nullable;

/// Combat state alternating between `AttackThrowPhase` strikes and optional `WalkingPhase` pursuit into attack range.
public final class Hunting implements UnitState {
  private static final float SECONDS_PER_ATTACK = 2f;

  private final Selectable<?> target;
  private final boolean canMove;
  private final UnitState fallbackState;
  private final GiveUpTracker.Single giveUpTracker = new GiveUpTracker.Single();
  private final AttackThrowPhase throwPhase = new AttackThrowPhase();

  public Hunting(Selectable<?> target, UnitState fallbackState) {
    this(target, true, fallbackState);
  }

  public Hunting(Selectable<?> target, boolean canMove, UnitState fallbackState) {
    this.target = target;
    this.canMove = canMove;
    this.fallbackState = fallbackState;
  }

  Hunting withFallback(UnitState newFallback) {
    return new Hunting(target, canMove, newFallback);
  }

  @Override
  public Transition decide(UnitContext context) {
    throwPhase.reset();
    if (target.isDead()) {
      return Transition.to(fallbackState);
    }
    if (context.isCloseEnough(context.getAttackRange(target), target)) {
      throwPhase.start(context);
      giveUpTracker.reset();
      return Transition.stay();
    }
    if (canMove && !giveUpTracker.shouldGiveUp()) {
      context.initWalkTarget(target, context.getAttackRange(target));
      return Transition.walk(false);
    }
    return Transition.to(fallbackState);
  }

  @Override
  public TickResult tick(float dt, UnitContext context) {
    return throwPhase.isActive() ? throwPhase.tick(dt, context, target) : TickResult.DONE;
  }

  boolean canMove() {
    return canMove;
  }

  boolean isThrowActive() {
    return throwPhase.isActive();
  }

  boolean isWeaponReleased() {
    return throwPhase.isReleased();
  }

  @Nullable Selectable<?> getActiveAttackTarget() {
    return throwPhase.isActive() ? target : null;
  }

  @Override
  public UnitState getFallbackState() {
    return fallbackState;
  }

  @Override
  public void resetGiveUpCounters() {
    giveUpTracker.reset();
  }

  /// Reusable weapon-throw animation and release cycle.
  private static final class AttackThrowPhase {
    private enum ThrowStage {
      IDLE,
      THROWING,
      RELEASED
    }

    private float animTime;
    private ThrowStage stage = ThrowStage.IDLE;

    void reset() {
      stage = ThrowStage.IDLE;
    }

    void start(UnitContext context) {
      float animSpeed = 1f / SECONDS_PER_ATTACK;
      animTime = context.getWeaponSecondsPerRelease(animSpeed);
      stage = ThrowStage.THROWING;
      context.switchAnimation(animSpeed, Unit.Animation.THROWING);
    }

    boolean isActive() {
      return stage != ThrowStage.IDLE;
    }

    TickResult tick(float dt, UnitContext context, Selectable<?> target) {
      animTime -= dt;
      context.aimAtTarget(target);
      return switch (stage) {
        case IDLE -> TickResult.DONE;
        case THROWING -> {
          if (animTime <= 0) {
            context.launchWeaponAttack(target);
            animTime += SECONDS_PER_ATTACK - context.getWeaponSecondsPerRelease(1f / SECONDS_PER_ATTACK);
            stage = ThrowStage.RELEASED;
          }
          yield TickResult.UNINTERRUPTIBLE;
        }
        case RELEASED -> animTime > 0 ? TickResult.UNINTERRUPTIBLE : TickResult.DONE;
      };
    }

    boolean isReleased() {
      return stage == ThrowStage.RELEASED;
    }
  }
}
