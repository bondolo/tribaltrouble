package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.AnimatedAccessory;
import com.oddlabs.tt.simulation.model.Target;

/**
 * Accessory that triggers lightning strikes.
 */
interface LightningAccessory extends AnimatedAccessory {
    /**
     * Triggers a lightning strike effect at the specified target.
     *
     * @param target the strike target
     */
    void triggerStrike(Target target);
}
