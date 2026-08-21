package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Unit;

/**
 * Behaviour that plays the idle animation and handles the stun duration.
 */
public final class StunBehaviour implements Behaviour {
    private final StunController controller;
    private final Unit unit;

    public StunBehaviour(StunController controller, Unit unit) {
        this.controller = controller;
        this.unit = unit;
    }

    @Override
    public State animate(float t) {
        unit.switchToIdleAnimation();
        return !controller.shouldSleep(t) ? State.DONE : State.UNINTERRUPTIBLE;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public void forceInterrupted() {
    }

    @Override
    public void onCleanup() {
    }
}
