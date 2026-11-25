package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.animation.Animated;

/// Client-side animation driver for supply spawn visual progress.
final class SupplySpawnAnimation implements Animated {
  private final SupplyVisualModel visualModel;
  private final float limit;

  private float time = 0;

  SupplySpawnAnimation(SupplyVisualModel visualModel) {
    this(visualModel, visualModel.getSpawnDuration());
  }

  SupplySpawnAnimation(SupplyVisualModel visualModel, float limit) {
    this.visualModel = visualModel;
    this.limit = limit;
    visualModel.getModel().getWorld().getAnimationManagerGameTime().registerAnimation(this);
    visualModel.setSpawnProgress(0.0f);
  }

  @Override
  public void animate(float dt) {
    time = Math.min(time + dt, limit);
    visualModel.setSpawnProgress(time / limit);
    if (time >= limit) {
      visualModel.getModel().getWorld().getAnimationManagerGameTime().removeAnimation(this);
      visualModel.completeSpawn();
    }
  }
}
