package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.EntityActivity;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Target;
import org.jspecify.annotations.Nullable;

/// Drives a `Unit`'s `UnitState` lifecycle while enforcing zero-tick cascades, interruptibility gating, forced
/// preemption, and re-entrant transition safety.
public final class UnitStateMachine {
    private static final int MAX_ZERO_TICK_TRANSITIONS = 64;

    private final WalkingPhase walkingPhase = new WalkingPhase();
    private @Nullable UnitState defaultState;
    private UnitState currentState;
    private @Nullable UnitState pendingState;
    private @Nullable UnitState legacyRootOrder;
    private boolean shouldDecide;
    private UnitState.StepStatus lastStepStatus = UnitState.StepStatus.DONE;
    private int transitionEpoch;

    UnitStateMachine(UnitState initialState) {
        this.currentState = initialState;
    }

    public UnitStateMachine(UnitState initialState, UnitContext context) {
        this(initialState);
        this.defaultState = initialState;
        this.lastStepStatus = UnitState.StepStatus.INTERRUPTIBLE;
        resolveDecide(context);
    }

    private void commitState(UnitState next) {
        walkingPhase.stop();
        currentState = next;
        if (currentState == defaultState) {
            legacyRootOrder = null;
        }
        transitionEpoch++;
    }

    private static @Nullable UnitState findRootAboveDefault(UnitState state, UnitState defaultState) {
        UnitState cursor = state;
        for (int i = 0; i < MAX_ZERO_TICK_TRANSITIONS && cursor != null; i++) {
            UnitState fallback = cursor.getFallbackState();
            if (fallback == defaultState) {
                return cursor;
            }
            cursor = fallback;
        }
        return null;
    }

    public UnitState getPrimaryOrderState() {
        return legacyRootOrder != null ? legacyRootOrder : getActiveOrPendingState();
    }

    private void resolveDecide(UnitContext context) {
        for (int hops = 0; hops < MAX_ZERO_TICK_TRANSITIONS; hops++) {
            int epochBefore = transitionEpoch;
            UnitState previous = currentState;
            walkingPhase.stop();
            UnitState.Transition transition = previous.decide(context);
            if (transitionEpoch != epochBefore) {
                return;
            }
            switch (transition) {
                case UnitState.Transition.Stay _ -> {
                    return;
                }
                case UnitState.Transition.Walk(boolean scanAttack) -> {
                    walkingPhase.start(scanAttack);
                    return;
                }
                case UnitState.Transition.To(UnitState nextState) -> commitState(nextState);
                case UnitState.Transition.PopAndPush(UnitState fallbackState, var nextFactory) -> {
                    commitState(fallbackState);
                    resolveDecide(context);
                    if (!context.isDead()) {
                        pushOrder(nextFactory.apply(currentState), context);
                    }
                    return;
                }
            }
        }
        throw new IllegalStateException("Exceeded maximum zero-tick state transitions (" + MAX_ZERO_TICK_TRANSITIONS
                + ") at state " + currentState);
    }

    /// Requests a new player or AI order, clearing any legacy root-order tracking and applying immediately
    /// unless `UNINTERRUPTIBLE`.
    public void requestOrder(UnitState newOrder, UnitContext context) {
        legacyRootOrder = null;
        applyOrder(newOrder, context);
    }

    /// Pushes an order onto the current state without clearing the legacy index-1 primary order when one is
    /// already active.
    public void pushOrder(UnitState newOrder, UnitContext context) {
        if (context.isLegacyDeterministic() && legacyRootOrder == null && defaultState != null) {
            UnitState root = findRootAboveDefault(getActiveOrPendingState(), defaultState);
            if (root != null) {
                legacyRootOrder = root;
            }
        }
        applyOrder(newOrder, context);
    }

    private void applyOrder(UnitState newOrder, UnitContext context) {
        if (lastStepStatus != UnitState.StepStatus.UNINTERRUPTIBLE) {
            pendingState = null;
            shouldDecide = false;
            commitState(newOrder);
            resolveDecide(context);
        } else {
            pendingState = newOrder;
            shouldDecide = true;
        }
    }

    /// Requests re-evaluation of the current state, deferring if currently `UNINTERRUPTIBLE`.
    public void requestDecide(UnitContext context) {
        if (lastStepStatus != UnitState.StepStatus.UNINTERRUPTIBLE) {
            pendingState = null;
            shouldDecide = false;
            resolveDecide(context);
        } else {
            shouldDecide = true;
        }
    }

    public void replaceDefaultAndRequestOrder(UnitState newDefaultState, UnitContext context) {
        this.defaultState = newDefaultState;
        requestOrder(newDefaultState, context);
    }

    /// Immediately preempts the current state (bypassing `UNINTERRUPTIBLE`), invokes interruption cleanup, clears any
    /// deferred order, and resolves `preemptingState` in zero ticks.
    public void forceTransition(UnitState preemptingState, UnitContext context) {
        UnitState interrupted = currentState;
        pendingState = null;
        shouldDecide = false;
        lastStepStatus = UnitState.StepStatus.INTERRUPTIBLE;
        commitState(preemptingState);
        if (interrupted instanceof CastingMagic casting) {
            casting.interrupt();
        }
        resolveDecide(context);
    }

    public void animate(float dt, UnitContext context) {
        int epochBefore = transitionEpoch;
        UnitState.StepStatus previousStepStatus = lastStepStatus;
        UnitState active = currentState;
        UnitState.TickResult result = walkingPhase.isActive()
                ? walkingPhase.step(dt, context, active)
                : active.tick(dt, context);
        if (transitionEpoch != epochBefore) {
            return;
        }
        lastStepStatus = result.status();
        UnitState next = result.nextState();
        if (next != null) {
            if (next instanceof Hunting hunting) {
                if (shouldDecide && pendingState != null) {
                    if (context.isLegacyDeterministic()) {
                        pendingState.resetGiveUpCounters();
                        next = hunting.withFallback(pendingState);
                        if (legacyRootOrder == null && defaultState != null) {
                            UnitState root = findRootAboveDefault(next, defaultState);
                            if (root != null) {
                                legacyRootOrder = root;
                            }
                        }
                        pendingState = null;
                        shouldDecide = false;
                    } else {
                        next = null;
                    }
                } else {
                    currentState.resetGiveUpCounters();
                    if (context.isLegacyDeterministic() && legacyRootOrder == null && defaultState != null) {
                        UnitState root = findRootAboveDefault(next, defaultState);
                        if (root != null) {
                            legacyRootOrder = root;
                        }
                    }
                }
            }
            if (next != null) {
                commitState(next);
            }
        }
        switch (lastStepStatus) {
            case UNINTERRUPTIBLE -> {
                if (next != null) {
                    resolveDecide(context);
                }
            }
            case INTERRUPTIBLE -> {
                if (shouldDecide) {
                    applyPendingAndResolve(context);
                } else if (next != null) {
                    resolveDecide(context);
                    if (context.isLegacyDeterministic()
                            && result.idleAnimationAfterDecide()
                            && previousStepStatus != UnitState.StepStatus.UNINTERRUPTIBLE
                            && !context.isDead()) {
                        context.switchToIdleAnimation();
                    }
                }
            }
            case DONE -> {
                if (shouldDecide) {
                    applyPendingAndResolve(context);
                } else {
                    resolveDecide(context);
                }
            }
        }
    }

    private void applyPendingAndResolve(UnitContext context) {
        UnitState nextOrder = pendingState;
        pendingState = null;
        shouldDecide = false;
        if (nextOrder != null) {
            commitState(nextOrder);
        }
        resolveDecide(context);
    }

    public UnitState getCurrentState() {
        return currentState;
    }

    @Nullable
    UnitState getPendingState() {
        return pendingState;
    }

    public UnitState getActiveOrPendingState() {
        return pendingState != null ? pendingState : currentState;
    }

    public boolean isShouldDecide() {
        return shouldDecide;
    }

    UnitState.StepStatus getLastStepStatus() {
        return lastStepStatus;
    }

    public String getLastStepStatusName() {
        return lastStepStatus.name();
    }

    public EntityActivity getActivity() {
        if (isStunned()) {
            return EntityActivity.STUNNED;
        }
        if (isAttacking() || isCastingMagic()) {
            return EntityActivity.ACTIVE;
        }
        return getActivityForState(getPrimaryOrderState());
    }

    private static EntityActivity getActivityForState(UnitState state) {
        return switch (state) {
            case Idling _,Defending _ -> EntityActivity.IDLE;
            case Hunting _,CastingMagic _ -> EntityActivity.ACTIVE;
            case Dying _ -> EntityActivity.DEAD;
            case Stunned _ -> EntityActivity.STUNNED;
            case Gathering _ -> EntityActivity.GATHERING;
            case PlacingBuilding _ -> EntityActivity.PLACING_BUILDING;
            case Walking w -> w.isScanAttack() ? EntityActivity.ATTACKING_MOVING : EntityActivity.MOVING;
            case Harvesting h -> getActivityForState(h.getFallbackState());
            case EnteringBuilding _,Repairing _,Transporting _ -> EntityActivity.OTHER;
            case TestUnitState t -> t.getActivity();
        };
    }

    public boolean isBlocking() {
        if (walkingPhase.isActive()) {
            return walkingPhase.isBlocking();
        }
        return switch (currentState) {
            case Idling _,CastingMagic _,Stunned _ -> true;
            case Dying _ -> throw new IllegalStateException();
            case Hunting h -> !h.canMove() || h.isThrowActive();
            case Harvesting h -> h.isHarvestActive();
            case Repairing r -> r.isRepairActive();
            case Defending _,EnteringBuilding _,Gathering _,PlacingBuilding _,Transporting _,Walking _ ->
                false;
            case TestUnitState t -> t.isBlocking();
        };
    }

    public boolean isIdle() {
        return getPrimaryOrderState() instanceof Idling;
    }

    public boolean isDefending() {
        return isDefendingState(getPrimaryOrderState());
    }

    private static boolean isDefendingState(UnitState state) {
        return switch (state) {
            case Defending _ -> true;
            case Hunting h -> isDefendingState(h.getFallbackState());
            case Stunned s -> isDefendingState(s.getFallbackState());
            default -> false;
        };
    }

    public boolean isPlacingBuilding() {
        return getPlacingBuildingTarget() != null;
    }

    public @Nullable Building getPlacingBuildingTarget() {
        return findPlacingBuildingTarget(getPrimaryOrderState());
    }

    private static @Nullable Building findPlacingBuildingTarget(UnitState state) {
        return switch (state) {
            case PlacingBuilding p -> p.getBuilding();
            case Hunting h -> findPlacingBuildingTarget(h.getFallbackState());
            case Stunned s -> findPlacingBuildingTarget(s.getFallbackState());
            case Harvesting h -> findPlacingBuildingTarget(h.getFallbackState());
            default -> null;
        };
    }

    public boolean isStunned() {
        return getActiveOrPendingState() instanceof Stunned;
    }

    public float getStunTimeLeft() {
        return getActiveOrPendingState() instanceof Stunned s ? s.getTimeLeft() : 0f;
    }

    public boolean isCastingMagic() {
        return isCastingMagicState(getPrimaryOrderState());
    }

    private static boolean isCastingMagicState(UnitState state) {
        return switch (state) {
            case CastingMagic _ -> true;
            case Stunned s -> isCastingMagicState(s.getFallbackState());
            default -> false;
        };
    }

    public boolean isAttacking() {
        return getActiveOrPendingState() instanceof Hunting
                || (currentState instanceof Hunting h && h.isThrowActive());
    }

    public boolean isAttackWeaponReleased() {
        return currentState instanceof Hunting h && h.isWeaponReleased();
    }

    public @Nullable Selectable<?> getAttackTarget() {
        return currentState instanceof Hunting h ? h.getActiveAttackTarget() : null;
    }

    public boolean isMoving() {
        return walkingPhase.isActive();
    }

    public float getOffsetZ() {
        return currentState instanceof Dying d ? d.getOffsetZ() : 0f;
    }

    public void resetGiveUpCounters() {
        getActiveOrPendingState().resetGiveUpCounters();
    }

    public @Nullable SupplyType getGatheringSupplyType() {
        return findGatheringSupplyType(getPrimaryOrderState());
    }

    private static @Nullable SupplyType findGatheringSupplyType(UnitState state) {
        return switch (state) {
            case Gathering g -> g.getSupplyType();
            case Hunting h -> findGatheringSupplyType(h.getFallbackState());
            case Stunned s -> findGatheringSupplyType(s.getFallbackState());
            case Harvesting h -> findGatheringSupplyType(h.getFallbackState());
            default -> null;
        };
    }

    public @Nullable Target getWalkTarget() {
        return getActiveOrPendingState() instanceof Walking w ? w.getTarget() : null;
    }

    public boolean isAggressiveWalking() {
        return isAggressiveWalkingState(getPrimaryOrderState());
    }

    private static boolean isAggressiveWalkingState(UnitState state) {
        return switch (state) {
            case Walking w -> w.isScanAttack();
            case Hunting h -> isAggressiveWalkingState(h.getFallbackState());
            case Stunned s -> isAggressiveWalkingState(s.getFallbackState());
            default -> false;
        };
    }
}
