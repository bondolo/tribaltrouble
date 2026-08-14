package com.oddlabs.tt.engine.render;

/**
 * Interface for accessories that can trigger lightning strikes.
 */
public interface LightningAccessory extends AnimatedAccessory {
    /**
     * Triggers a lightning strike effect at the specified target coordinates.
     *
     * @param targetX target x coordinate
     * @param targetY target y coordinate
     * @param targetZ target z coordinate
     */
    void triggerStrike(float targetX, float targetY, float targetZ);
}
