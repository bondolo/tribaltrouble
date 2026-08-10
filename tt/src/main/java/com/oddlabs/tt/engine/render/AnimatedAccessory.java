package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.effects.render.*;

import com.oddlabs.tt.core.animation.Animated;

/**
 * An {@link Accessory} that requires continuous animation updates.
 * Used for dynamic visuals like particle emitters or cycling animations.
 */
public non-sealed interface AnimatedAccessory extends Accessory, Animated {
}
