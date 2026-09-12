package com.oddlabs.tt.simulation.behaviour;


/** No-op behaviour used when no active behaviour is assigned. */
final class NullBehaviour implements Behaviour {
    @Override
    public State animate(float dt) {
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
