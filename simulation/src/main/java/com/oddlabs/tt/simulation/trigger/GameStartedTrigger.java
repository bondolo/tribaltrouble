package com.oddlabs.tt.simulation.trigger;

import com.oddlabs.tt.simulation.landscape.World;

/**
 * Triggers an introductory action shortly after the game starts.
 */
public final class GameStartedTrigger extends IntervalTrigger {
    private final Runnable runnable;

    public GameStartedTrigger(World world, Runnable runnable) {
        super(world, .25f, 0f);
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
