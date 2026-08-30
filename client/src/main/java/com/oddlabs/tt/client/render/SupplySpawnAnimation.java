package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.simulation.model.SupplyModel;

/**
 * Client-side animation driver for supply spawn visual progress.
 */
public final class SupplySpawnAnimation implements Animated {
    private final SupplyVisualModel visualModel;
    private final float limit;

    private float time = 0;

    public SupplySpawnAnimation(SupplyVisualModel visualModel) {
        this(visualModel, visualModel.getSpawnDuration());
    }

    public SupplySpawnAnimation(SupplyVisualModel visualModel, float limit) {
        this.visualModel = visualModel;
        this.limit = limit;
        visualModel.getModel().getWorld().getAnimationManagerGameTime().registerAnimation(this);
        visualModel.setSpawnProgress(0.0f);
    }

    @Override
    public void animate(float t) {
        time = Math.min(time + t, limit);
        visualModel.setSpawnProgress(time / limit);
        if (time >= limit) {
            visualModel.getModel().getWorld().getAnimationManagerGameTime().removeAnimation(this);
            visualModel.completeSpawn();
        }
    }
}
