package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.animation.Animated;

public sealed interface Magic extends Animated permits LightningCloud, PoisonFog, SonicBlast, Stun {
    void interrupt();
}
