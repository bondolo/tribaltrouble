package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Unit;

/**
 * Behaviour that plays the idle animation and handles the stun duration.
 */
final class StunBehaviour implements Behaviour {
    private final StunController controller;
    private final Unit unit;

    StunBehaviour(StunController controller, Unit unit) {
        this.controller = controller;
        this.unit = unit;
    }

    @Override
    public State animate(float dt) {
        unit.switchToIdleAnimation();
        return !controller.shouldSleep(dt) ? State.DONE : State.UNINTERRUPTIBLE;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public void forceInterrupted() {
    }
}
