package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.simulation.model.SupplyModel;

/**
 * Client-side animation driver for supply spawn visual progress.
 */
public final class SupplySpawnAnimation implements Animated {
    private final SupplyModel supply;
    private final float limit;

    private float time = 0;

    public SupplySpawnAnimation(SupplyModel supply) {
        this(supply, SupplyVisualState.getSpawnDuration(supply));
    }

    public SupplySpawnAnimation(SupplyModel supply, float limit) {
        this.supply = supply;
        this.limit = limit;
        supply.getWorld().getAnimationManagerGameTime().registerAnimation(this);
        SupplyVisualState.registerSpawn(supply, limit);
    }

    @Override
    public void animate(float t) {
        time = Math.min(time + t, limit);
        SupplyVisualState.updateSpawn(supply, time / limit);
        if (time >= limit) {
            supply.getWorld().getAnimationManagerGameTime().removeAnimation(this);
            SupplyVisualState.completeSpawn(supply);
        }
    }
}
