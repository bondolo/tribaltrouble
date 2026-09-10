package com.oddlabs.tt.simulation.trigger;

import com.oddlabs.tt.simulation.landscape.World;

/** Triggers when a certain amount of time has passed */
public final class TimeTrigger extends IntervalTrigger {
    private final Runnable runnable;

    public TimeTrigger(World world, float time, Runnable runnable) {
        super(time, 0f, world.getAnimationManagerGameTime());
        this.runnable = runnable;
    }

    @Override
    protected void check() {
        triggered();
    }

    @Override
    protected void done() {
        runnable.run();
    }
}
