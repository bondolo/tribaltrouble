package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.AnimatedAccessory;

/**
 * Accessory that triggers sonic shockwave effects.
 */
interface SonicBlastAccessory extends AnimatedAccessory {
    /**
     * Triggers a sonic blast shockwave effect.
     */
    void triggerBlast();
}
