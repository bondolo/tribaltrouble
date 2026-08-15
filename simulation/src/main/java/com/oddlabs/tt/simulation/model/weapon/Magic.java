package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.base.animation.Animated;

/**
 * Interface for active magic effects in the world.
 */
public sealed interface Magic extends Animated permits LightningCloud, PoisonFog, SonicBlast, Stun {
    void interrupt();
}
