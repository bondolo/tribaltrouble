package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.AttackScanFilter;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.pathfinder.Movable;
import com.oddlabs.tt.simulation.pathfinder.PathTracker;
import org.jspecify.annotations.Nullable;

/// Reusable pathfinding execution phase handling step progression, blocker exponential backoff, and aggro scanning.
final class WalkingPhase {
    private static final float WAIT_RETRY_DELAY = 0.5f;
    private static final float MAX_WAIT_RETRY_DELAY = 5f;

    private boolean active;
    private boolean scanAttack;
    private @Nullable Movable blockingMovable;
    private int blockerX;
    private int blockerY;
    private float retryDelayCounter;
    private float retryDelay = WAIT_RETRY_DELAY;
    private PathTracker.State state = PathTracker.State.DONE;

    WalkingPhase() {
    }

    void start(boolean scanAttack) {
        this.active = true;
        this.scanAttack = scanAttack;
        this.blockingMovable = null;
        this.retryDelayCounter = 0f;
        this.retryDelay = WAIT_RETRY_DELAY;
        this.state = PathTracker.State.DONE;
    }

    void stop() {
        this.active = false;
        this.blockingMovable = null;
        this.state = PathTracker.State.DONE;
    }

    boolean isActive() {
        return active;
    }

    boolean isBlocking() {
        return state == PathTracker.State.BLOCKED;
    }

    float getRetryDelay() {
        return retryDelay;
    }

    UnitState.TickResult step(float dt, UnitContext context, UnitState ownerState) {
        retryDelayCounter -= dt;
        boolean blockerMoved = blockingMovable != null
                && (blockingMovable.getGridX() != blockerX || blockingMovable.getGridY() != blockerY);
        if (retryDelayCounter > 0 && !blockerMoved) {
            return UnitState.TickResult.INTERRUPTIBLE;
        }
        retryDelayCounter = 0f;
        blockingMovable = null;
        state = context.stepWalk(dt);
        return switch (state) {
            case OK -> {
                context.switchToMovingAnimation();
                yield UnitState.TickResult.UNINTERRUPTIBLE;
            }
            case OK_INTERRUPTIBLE -> {
                retryDelay = WAIT_RETRY_DELAY;
                context.switchToMovingAnimation();
                yield scanOrContinue(context, ownerState);
            }
            case DONE -> UnitState.TickResult.DONE;
            case BLOCKED, SOFTBLOCKED -> {
                Movable blocker = context.getCurrentWalkBlocker();
                if (blocker != null) {
                    blockingMovable = blocker;
                    if (!blockingMovable.isDead()) {
                        blockingMovable.markBlocking();
                        blockerX = blockingMovable.getGridX();
                        blockerY = blockingMovable.getGridY();
                    } else {
                        blockingMovable = null;
                    }
                }
                UnitState.TickResult scanResult = scanOrContinue(context, ownerState);
                retryDelay = Math.min(2f * retryDelay, MAX_WAIT_RETRY_DELAY);
                retryDelayCounter = retryDelay;
                context.switchToIdleAnimation();
                yield scanResult.nextState() != null
                        ? UnitState.TickResult.interruptibleWithPostIdleAnimation(scanResult.nextState())
                        : scanResult;
            }
        };
    }

    private UnitState.TickResult scanOrContinue(UnitContext context, UnitState ownerState) {
        if (scanAttack) {
            Selectable<?> enemy = context.scanForEnemy(AttackScanFilter.UNIT_RANGE);
            if (enemy != null) {
                return UnitState.TickResult.interruptible(new Hunting(enemy, ownerState));
            }
        }
        return UnitState.TickResult.INTERRUPTIBLE;
    }
}
