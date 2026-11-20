package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.Target;

/// Movement order state walking toward `target` with optional attack-move enemy scanning.
public final class Walking implements UnitState {
    private final Target target;
    private final boolean scanAttack;
    private final UnitState fallbackState;
    private final GiveUpTracker.Single giveUpTracker = new GiveUpTracker.Single();

    public Walking(Target target, boolean scanAttack, UnitState fallbackState) {
        this.target = target;
        this.scanAttack = scanAttack;
        this.fallbackState = fallbackState;
    }

    @Override
    public Transition decide(UnitContext context) {
        if (giveUpTracker.shouldGiveUp()) {
            return Transition.to(fallbackState);
        }
        context.initWalkTarget(target, 0f);
        return Transition.walk(scanAttack);
    }

    @Override
    public UnitState getFallbackState() {
        return fallbackState;
    }

    Target getTarget() {
        return target;
    }

    boolean isScanAttack() {
        return scanAttack;
    }

    @Override
    public void resetGiveUpCounters() {
        giveUpTracker.reset();
    }
}
