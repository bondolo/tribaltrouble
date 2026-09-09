package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Unit;

public final class IdleBehaviour implements Behaviour {
    private final IdleController controller;
    private final Unit unit;

    public IdleBehaviour(IdleController controller, Unit unit) {
        this.controller = controller;
        this.unit = unit;
    }

    @Override
    public State animate(float dt) {
        unit.switchToIdleAnimation();
        return controller.shouldSleep(dt) ? State.INTERRUPTIBLE : State.DONE;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public void forceInterrupted() {
    }
}
