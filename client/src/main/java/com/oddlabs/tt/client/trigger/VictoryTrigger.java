package com.oddlabs.tt.client.trigger;

import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.trigger.IntervalTrigger;
import com.oddlabs.tt.client.viewer.WorldViewer;

import java.util.List;

/**
 * Monitors campaign levels to check for the victory condition, which is triggered
 * when all enemy units are eliminated and the enemy has no active chieftain.
 */
public final class VictoryTrigger extends IntervalTrigger {
    private final WorldViewer viewer;
    private final Runnable runnable;

    public VictoryTrigger(WorldViewer viewer, Runnable runnable) {
        super(viewer.getWorld(), .5f, 0f);
        this.viewer = viewer;
        this.runnable = runnable;
    }

    @Override
    protected void check() {
        List<Player> players = viewer.getWorld().getPlayers();
        Player local = viewer.getLocalPlayer();

        for (Player current : players) {
            if (local.isEnemy(current)) {
                int units = current.getUnitCountContainer().getNumSupplies();
                if (units > 0 || current.hasActiveChieftain()) {
                    return;
                }
            }
        }
        triggered();
    }

    @Override
    protected void done() {
        runnable.run();
    }
}
