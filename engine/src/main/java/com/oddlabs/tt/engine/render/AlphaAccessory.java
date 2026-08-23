package com.oddlabs.tt.engine.render;

/**
 * An accessory that provides dynamic alpha transparency.
 */
public interface AlphaAccessory extends AnimatedAccessory {
    /**
     * Returns the current alpha transparency multiplier (0.0 to 1.0).
     *
     * @return the current alpha transparency multiplier (0.0 to 1.0)
     */
    float getAlpha();
}
