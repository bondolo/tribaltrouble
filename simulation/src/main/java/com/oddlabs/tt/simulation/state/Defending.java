package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.Target;

/// Defensive march state returning to a guarded position while scanning for enemies.
public final class Defending implements UnitState {
    private final Target target;
    private final UnitState fallbackState;
    private final GiveUpTracker.Single giveUpTracker = new GiveUpTracker.Single();

    public Defending(Target target, UnitState fallbackState) {
        this.target = target;
        this.fallbackState = fallbackState;
    }

    @Override
    public Transition decide(UnitContext context) {
        if (giveUpTracker.shouldGiveUp()) {
            return Transition.to(fallbackState);
        }
        context.initWalkTarget(target, 0f);
        return Transition.walk(true);
    }

    @Override
    public UnitState getFallbackState() {
        return fallbackState;
    }

    @Override
    public void resetGiveUpCounters() {
        giveUpTracker.reset();
    }
}
