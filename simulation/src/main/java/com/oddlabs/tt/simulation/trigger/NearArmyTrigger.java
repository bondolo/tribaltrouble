package com.oddlabs.tt.simulation.trigger;

import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.pathfinder.CountOccupantScanFilter;
import com.oddlabs.tt.simulation.player.Player;

public final class NearArmyTrigger extends IntervalTrigger {
    private final Unit[] src;
    private final float r;
    private final Player player;
    private final Runnable runnable;

    public NearArmyTrigger(Unit[] src, float r, Player player, Runnable runnable) {
        super(player.getWorld(), .25f, 0f);
        this.src = src;
        this.r = r;
        this.player = player;
        this.runnable = runnable;
    }

    @Override
    protected void check() {
        for (Unit unit : src) {
            if (unit.isDead()) {
                continue;
            }
            CountOccupantScanFilter<Unit> filter = new CountOccupantScanFilter<>(unit.getPositionX(),
                    unit.getPositionY(), r, unit, Unit.class,
                    filtered -> filtered.isAlive() && filtered.getOwner() == player, 1);
            player.getWorld().getUnitGrid().scan(filter, unit.getGridX(), unit.getGridY());
            if (filter.hasMatch()) {
                triggered();
                return;
            }
        }
    }

    @Override
    protected void done() {
        runnable.run();
    }
}
