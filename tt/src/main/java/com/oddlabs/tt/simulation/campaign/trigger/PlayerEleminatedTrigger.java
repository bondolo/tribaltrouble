package com.oddlabs.tt.simulation.campaign.trigger;

import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.trigger.IntervalTrigger;
import org.jspecify.annotations.NonNull;

public final class PlayerEleminatedTrigger extends IntervalTrigger {
    private final Runnable runnable;
    private final @NonNull Player player;

    public PlayerEleminatedTrigger(Runnable runnable, @NonNull Player player) {
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
