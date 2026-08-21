package com.oddlabs.tt.simulation.trigger;

import com.oddlabs.tt.simulation.player.Player;

public final class PlayerEleminatedTrigger extends IntervalTrigger {
    private final Runnable runnable;
    private final Player player;

    public PlayerEleminatedTrigger(Runnable runnable, Player player) {
        super(player.getWorld(), .5f, 0f);
        this.runnable = runnable;
        this.player = player;
    }

    @Override
    protected void check() {
        int units = player.getUnitCountContainer().getNumSupplies();
        if (units == 0 && !player.hasActiveChieftain()) {
            triggered();
        }
    }

    @Override
    protected void done() {
        runnable.run();
    }
}
