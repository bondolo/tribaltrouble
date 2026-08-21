package com.oddlabs.tt.simulation.behaviour;


public final class NullBehaviour implements Behaviour {
    @Override
    public State animate(float t) {
        return State.INTERRUPTIBLE;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public void forceInterrupted() {
    }
}
