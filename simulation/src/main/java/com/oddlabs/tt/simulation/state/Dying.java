package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.Unit;

/// Death animation, corpse pause, and terrain-sink sequence before removing a unit from the world.
public final class Dying implements UnitState {
    private static final float SECONDS_PER_DEATH = 3f;
    private static final float LYING_SECONDS = 1f;
    private static final float MOVING_SECONDS = 60f;
    private static final int MOVING_METERS = 3;

    private enum Phase {
        DYING,
        LYING,
        MOVING
    }

    private float animTime = SECONDS_PER_DEATH;
    private Phase phase = Phase.DYING;
    private float offsetZ;
    private float dz;
    private boolean initialized;

    @Override
    public Transition decide(UnitContext context) {
        if (!initialized) {
            context.switchAnimation(1f / animTime, Unit.Animation.DYING);
            initialized = true;
        }
        return Transition.stay();
    }

    @Override
    public TickResult tick(float dt, UnitContext context) {
        animTime -= dt;
        offsetZ -= dz * dt;
        context.setOffsetZ(offsetZ);
        if (animTime < 0) {
            switch (phase) {
                case DYING -> {
                    animTime += LYING_SECONDS;
                    phase = Phase.LYING;
                }
                case LYING -> {
                    animTime += MOVING_SECONDS;
                    phase = Phase.MOVING;
                    dz = MOVING_METERS / MOVING_SECONDS;
                }
                case MOVING -> context.removeFromWorld();
            }
        }
        return TickResult.UNINTERRUPTIBLE;
    }

    float getOffsetZ() {
        return offsetZ;
    }
}
