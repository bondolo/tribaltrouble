package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.AnimatedAccessory;
import com.oddlabs.tt.simulation.model.Target;

/// Accessory that triggers lightning strikes.
interface LightningAccessory extends AnimatedAccessory {
  void triggerStrike(Target target);
}
