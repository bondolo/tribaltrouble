package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Selectable;

/// Default resting state that periodically scans for nearby enemies and initiates combat or leashed pursuit.
public final class Idling implements UnitState {
    private static final float MIN_SCAN_DELAY = 1f;
    private static final float MAX_SCAN_DELAY = 2f;

    private final int scanRange;
    private final boolean canMove;
    private float redecideTime;

    public Idling(int scanRange, boolean canMove) {
        this.scanRange = scanRange;
        this.canMove = canMove;
    }

    private boolean shouldSleep(float dt) {
        redecideTime -= dt;
        return redecideTime > 0;
    }

    @Override
    public Transition decide(UnitContext context) {
        if (shouldSleep(0f)) {
            return Transition.stay();
        }
        boolean canAttack = context.hasAbility(Abilities.ATTACK);
        if (canAttack || context.isLegacyDeterministic()) {
            redecideTime = MIN_SCAN_DELAY + context.nextRandomFloat() * (MAX_SCAN_DELAY - MIN_SCAN_DELAY);
        } else {
            redecideTime = MAX_SCAN_DELAY;
        }
        Selectable<?> enemy = canAttack ? context.scanForEnemy(scanRange) : null;
        if (enemy != null) {
            if (canMove) {
                Walking leashWalk = new Walking(
                        new LandscapeTarget(context.getGridX(), context.getGridY()), true, this);
                return Transition.to(new Hunting(enemy, leashWalk));
            }
            return Transition.to(new Hunting(enemy, false, this));
        }
        return Transition.stay();
    }

    @Override
    public TickResult tick(float dt, UnitContext context) {
        context.switchToIdleAnimation();
        return shouldSleep(dt) ? TickResult.INTERRUPTIBLE : TickResult.DONE;
    }
}
