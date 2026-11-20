package com.oddlabs.tt.simulation.state;

/// Temporary incapacitation state that freezes the unit and resumes `fallbackState` upon expiry.
public final class Stunned implements UnitState {
    private float time;
    private final UnitState fallbackState;

    public Stunned(float time, UnitState fallbackState) {
        this.time = time;
        this.fallbackState = fallbackState;
    }

    /// Creates a stun state preempting `interruptedState`; in legacy deterministic mode nested stuns stack additively,
    /// whereas modern mode refreshes to the maximum remaining duration.
    public static Stunned preempt(float duration, UnitState interruptedState, boolean legacyDeterministic) {
        if (!legacyDeterministic && interruptedState instanceof Stunned existing) {
            return new Stunned(Math.max(duration, existing.time), existing.fallbackState);
        }
        return new Stunned(duration, interruptedState);
    }

    private boolean shouldSleep(float dt) {
        time -= dt;
        return time > 0;
    }

    @Override
    public Transition decide(UnitContext context) {
        if (!shouldSleep(0f)) {
            return Transition.to(fallbackState);
        }
        return Transition.stay();
    }

    @Override
    public TickResult tick(float dt, UnitContext context) {
        context.switchToIdleAnimation();
        return !shouldSleep(dt) ? TickResult.DONE : TickResult.UNINTERRUPTIBLE;
    }

    float getTimeLeft() {
        return time;
    }

    @Override
    public UnitState getFallbackState() {
        return fallbackState;
    }
}
